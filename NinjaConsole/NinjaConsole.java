package NinjaConsole;

import java.awt.BorderLayout;
import java.awt.Color;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.IOException;

import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;

import javax.swing.JFrame;

public class NinjaConsole extends JPanel implements
    LogConstants, ActionListener, Runnable, ScrollPaneConstants
{
    private static final Color [] FontColours =
    {
        null, //new Color(0.0f, 0.0f, 0.0f), // Style_Output
        new Color(0.0f, 0.0f, 0.5f), // Style_Input
        new Color(0.5f, 0.0f, 0.0f), // Style_Error
        new Color(0.0f, 0.5f, 0.0f), // Style_System
        new Color(0.0f, 0.5f, 0.5f), // Style_Interactive
        new Color(0.5f, 0.5f, 0.5f), // Style_Debug
    };

    private JScrollPane m_scrollPane;
    private JTextPane m_textPane;
    private JTextField m_textField;

    private Style [] m_styles;

    private Process m_process;
    private BufferedWriter m_input;
    private BufferedReader m_output;
    private BufferedReader m_error;
    private Thread m_logger;

    private boolean m_interactive;

    public NinjaConsole()
    {
        initComponents();
        initStyles();

        log(Style_System, "NinjaConsole v0.1");
    }

    private void initComponents()
    {
        m_textPane = new JTextPane();
        m_textPane.setEditable(false);

        m_scrollPane = new JScrollPane(m_textPane);
        m_scrollPane.setHorizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_NEVER);
        m_scrollPane.setVerticalScrollBarPolicy(VERTICAL_SCROLLBAR_AS_NEEDED);

        m_textField = new JTextField();
        m_textField.addActionListener(this);

        setLayout(new BorderLayout());
        add(m_scrollPane, BorderLayout.CENTER);
        add(m_textField, BorderLayout.SOUTH);
    }
    private void initStyles()
    {
        m_styles = new Style[Style_Count];
        StyledDocument doc = m_textPane.getStyledDocument();

        Style baseFont = doc.addStyle("base_font", null);
        StyleConstants.setFontFamily(baseFont, "Monospaced");
        m_styles[Style_Output] = baseFont;

        for (int i = 1; i < Style_Count; ++i)
        {
            Style s = doc.addStyle(null, baseFont);
            StyleConstants.setForeground(s, FontColours[i]);
            m_styles[i] = s;
        }
    }

    //
    // Config Options
    //

    public boolean getWordWrap()
    {
        return m_scrollPane.getHorizontalScrollBarPolicy() == HORIZONTAL_SCROLLBAR_AS_NEEDED;
    }
    public void setWordWrap(boolean wrap)
    {
        int policy = wrap ? HORIZONTAL_SCROLLBAR_NEVER : HORIZONTAL_SCROLLBAR_AS_NEEDED;
        m_scrollPane.setHorizontalScrollBarPolicy(policy);
    }

    public boolean getInteractive()
    {
        return m_interactive;
    }
    public void setInteractive(boolean interactive)
    {
        m_interactive = interactive;
    }

    //
    // Public Usage Interface
    //

    public synchronized void log(int style, String message)
    {
        try
        {
            StyledDocument doc = m_textPane.getStyledDocument();
            Style s = m_styles[style];
            doc.insertString(doc.getLength(), message, s);
            doc.insertString(doc.getLength(), "\n", s);
        }
        catch (BadLocationException e)
        {
            System.err.println(e);
        }
    }

    public void startProcess(String workingDir, String command)
    {
        try
        {
            Runtime r = Runtime.getRuntime();
            setProcess(r.exec(command, null, new File(workingDir)));
        }
        catch (IOException e)
        {
            log(Style_Error, e.toString());
        }
    }
    private void setProcess(Process p)
    {
        if (m_logger != null && m_logger.isAlive())
        {
            log(Style_Error, "Cannot orphan current process.");
            return;
        }

        m_process = p;
        if (p == null)
        {
            m_input = null;
            m_output = null;
            m_error = null;
            m_logger = null;
        }
        else
        {
            m_input = new BufferedWriter(new OutputStreamWriter(p.getOutputStream()));
            m_output = new BufferedReader(new InputStreamReader(p.getInputStream()));
            m_error = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            m_logger = new Thread(this);
            m_logger.start();
        }
    }

    //
    // Interface Implementations
    //

    public void run()
    {
        while (m_output != null && m_error != null && m_logger != null)
        {
            try
            {
                while (m_output.ready())
                {
                    log(Style_Output, m_output.readLine());
                }
            }
            catch (IOException e) { System.err.println(e); }

            try
            {
                while (m_error.ready())
                {
                    log(Style_Error, m_error.readLine());
                }
            }
            catch (IOException e) { System.err.println(e); }

            try
            {
                m_process.exitValue();
                m_logger = null;
            }
            catch (IllegalThreadStateException e) { }

            Thread.yield();
        }

        try
        {
            log(Style_System, "Process exited (" + m_process.exitValue() + ").");
            setProcess(null);
        }
        catch (IllegalThreadStateException e)
        {
            log(Style_Error, "Logger terminated with process still running.");
        }
    }

    public void actionPerformed(ActionEvent event)
    {
        if (m_process != null)
        {
            try
            {
                m_process.exitValue();
                setProcess(null);
            }
            catch (IllegalThreadStateException e) { }
        }

        String command = m_textField.getText();
        m_textField.setText("");

        if (m_input != null && m_process != null)
        {
            log(Style_Input, command);
            try
            {
                m_input.write(command, 0, command.length());
                m_input.newLine();
            }
            catch (IOException e) { }
        }
        else if (m_interactive)
        {
            log(Style_Interactive, command);
            startProcess(".", command);
        }
        else
        {
            log(Style_Error, command);
        }
    }

    //
    // Temporary Hook
    //

    public static void main(String [] args)
    {
        JFrame window = new JFrame();
        NinjaConsole console = new NinjaConsole();
        console.setInteractive(true);
        window.setContentPane(console);
        window.setSize(640, 480);
        window.setLocation(640, 480);
        window.setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        window.setTitle("NinjaConsole");
        window.setVisible(true);

        String workingDir = "/Users/abubics/Development/git_sandbox_mvpss/devices_ember/em35x-ezsp/build/smartmeter";
        String command = "./smartmeter -p /dev/cu.SLAB_USBtoUART -o 0";
        console.startProcess(workingDir, command);
    }
}
