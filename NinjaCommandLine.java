import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.IOException;

import javax.swing.JTextField;

public class NinjaCommandLine extends JTextField implements LogConstants, ActionListener
{
    private NinjaConsole m_parent;

    private Process m_process;
    private BufferedWriter m_inputWriter;

    public NinjaCommandLine(NinjaConsole parent)
    {
        super();
        m_parent = parent;
        addActionListener(this);
    }

    public void setProcess(Process p)
    {
        m_process = p;
        if (p == null)
        {
            //printStack("set null process");
            m_inputWriter = null;
        }
        else
        {
            //printStack("set non-null process");
            m_inputWriter = new BufferedWriter(new OutputStreamWriter(p.getOutputStream()));
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

        String command = getText();
        setText("");

        if (m_inputWriter != null && m_process != null)
        {
            m_parent.log(Style_Input, command);
            try
            {
                m_inputWriter.write(command, 0, command.length());
                m_inputWriter.newLine();
            }
            catch (IOException e) { }
        }
        else
        {
            //m_parent.log(Style_Error, command);
            m_parent.log(Style_System, command);
            m_parent.startProcess(".", command);
        }
    }

    private static void printStack(String message)
    {
        System.err.print(message + ": ");
        try { throw new Exception(); }
        catch (Exception e) { e.printStackTrace(); }
    }
}
