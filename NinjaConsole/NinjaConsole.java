package NinjaConsole;

import java.awt.BorderLayout;
import java.awt.Color;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.io.BufferedWriter;
import java.io.File;
import java.io.InputStream;
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

public class NinjaConsole extends JPanel implements
    LogConstants, ActionListener, Runnable, ScrollPaneConstants
{
    private static final Color [] FontColours =
    {
        null,                        // Style_Output
        new Color(0.0f, 0.2f, 0.8f), // Style_Input
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
    private InputStream m_output;
    private InputStream m_error;
    private Thread m_logger;

    private boolean m_interactive;

    public NinjaConsole()
    {
        initComponents();
        initStyles();

        log(Style_System, "NinjaConsole v0.1\n");
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
            doc.insertString(doc.getLength(), message, m_styles[style]);
        }
        catch (BadLocationException e)
        {
            System.err.println(e);
        }
    }
    private void log(int style, char c)
    {
        log(style, String.valueOf(c));
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
            log(Style_Error, "\n");
        }
    }
    private void setProcess(Process p)
    {
        if (m_logger != null && m_logger.isAlive())
        {
            log(Style_Error, "Cannot orphan current process.\n");
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
            m_output = p.getInputStream();
            m_error = p.getErrorStream();
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
            logAvailableBytes(m_output, Style_Output);
            Thread.yield();
            logAvailableBytes(m_error, Style_Error);
            Thread.yield();

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
            log(Style_System, "Process exited (" + m_process.exitValue() + ").\n");
            setProcess(null);
        }
        catch (IllegalThreadStateException e)
        {
            log(Style_Error, "Logger terminated with process still running.\n");
        }
    }
    private static final byte [] s_tempBytes = new byte[128];
    private void logAvailableBytes(InputStream in, int style)
    {
        try
        {
            int available = in.available();
            int len;
            while (available > 0)
            {
                len = in.read(s_tempBytes);
                if (len > 0)
                {
                    log(style, new String(s_tempBytes, 0, len));
                }
                available = in.available();
            }
        }
        catch (IOException e) { System.err.println(e); }
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

        String command = m_textField.getText() + "\n";
        m_textField.setText("");

        if (m_input != null && m_process != null)
        {
            log(Style_Input, command);
            try
            {
                m_input.write(command, 0, command.length());
                m_input.flush();
            }
            catch (IOException e) { }
        }
        else if (m_interactive)
        {
            log(Style_Interactive, command);
            startProcess(".", command.trim());
        }
        else
        {
            log(Style_Error, command);
        }
    }
}
