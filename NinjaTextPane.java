import java.awt.Color;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

public class NinjaTextPane extends JTextPane implements LogConstants, Runnable
{
    private Style [] m_styles;

    private Process m_process;
    private BufferedReader m_outputReader;
    private BufferedReader m_errorReader;
    private Thread m_logger;

    public NinjaTextPane()
    {
        super();
        setEditable(false);

        m_styles = new Style[Style_Count];
        StyledDocument doc = getStyledDocument();

        m_styles[Style_Output] = doc.addStyle("base_font", null);
        StyleConstants.setFontFamily(m_styles[Style_Output], "Monospaced");

        m_styles[Style_Input] = doc.addStyle("input_font", m_styles[Style_Output]);
        StyleConstants.setForeground(m_styles[Style_Input], new Color(0, 0, 0.5f));

        m_styles[Style_Error] = doc.addStyle("error_font", m_styles[Style_Output]);
        StyleConstants.setForeground(m_styles[Style_Error], new Color(0.5f, 0, 0));

        m_styles[Style_System] = doc.addStyle("error_font", m_styles[Style_Output]);
        StyleConstants.setForeground(m_styles[Style_System], new Color(0, 0.5f, 0));

        log(Style_System, "NinjaConsole v0.1");
    }

    public synchronized void log(int style, String message)
    {
        try
        {
            StyledDocument doc = getStyledDocument();
            doc.insertString(doc.getLength(), message + "\n", m_styles[style]);
        }
        catch (BadLocationException e)
        {
            System.err.println(e);
        }
    }

    public void setProcess(Process p)
    {
        if (m_logger != null && m_logger.isAlive())
        {
            log(Style_Error, "Cannot orphan current process.");
            return;
        }

        m_process = p;
        if (p == null)
        {
            m_outputReader = null;
            m_errorReader = null;
            m_logger = null;
        }
        else
        {
            m_outputReader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            m_errorReader = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            m_logger = new Thread(this);
            m_logger.start();
        }
    }

    public void run()
    {
        while (m_outputReader != null && m_errorReader != null && m_logger != null)
        {
            try
            {
                while (m_outputReader.ready())
                {
                    String line = m_outputReader.readLine();
                    log(Style_Output, line);
                }
            }
            catch (IOException e) { System.err.println(e); }

            try
            {
                while (m_errorReader.ready())
                {
                    String line = m_errorReader.readLine();
                    log(Style_Error, line);
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
}
