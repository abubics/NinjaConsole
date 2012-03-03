import java.awt.BorderLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import javax.swing.JFrame;

public class NinjaConsole extends JPanel implements ScrollPaneConstants
{
    private NinjaTextPane m_textPane;
    private NinjaCommandLine m_commandLine;
    private JScrollPane m_scrollPane;

    public NinjaConsole()
    {
        m_textPane = new NinjaTextPane();
        m_commandLine = new NinjaCommandLine(this);

        m_scrollPane = new JScrollPane(m_textPane);
        m_scrollPane.setHorizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_NEVER);
        m_scrollPane.setVerticalScrollBarPolicy(VERTICAL_SCROLLBAR_AS_NEEDED);

        setLayout(new BorderLayout());
        add(m_scrollPane, BorderLayout.CENTER);
        add(m_commandLine, BorderLayout.SOUTH);
    }

    public void setWordWrap(boolean wrap)
    {
        int policy = wrap ? HORIZONTAL_SCROLLBAR_NEVER : HORIZONTAL_SCROLLBAR_AS_NEEDED;
        m_scrollPane.setHorizontalScrollBarPolicy(policy);
    }

    public void log(int style, String message)
    {
        m_textPane.log(style, message);
    }

    public NinjaTextPane getTextPane()
    {
        return m_textPane;
    }
    public NinjaCommandLine getCommandLine()
    {
        return m_commandLine;
    }

    public void startProcess(String workingDir, String command)
    {
        try
        {
            Runtime r = Runtime.getRuntime();
            setProcess(r.exec(command, null, new java.io.File(workingDir)));
        }
        catch (java.io.IOException e)
        {
            log(LogConstants.Style_Error, e.toString());
        }
    }
    private void setProcess(Process p)
    {
        m_textPane.setProcess(p);
        m_commandLine.setProcess(p);
    }

    public static void main(String [] args)
    {
        JFrame window = new JFrame();
        NinjaConsole console = new NinjaConsole();
        window.setContentPane(console);
        window.setSize(640, 480);
        window.setLocation(640, 480);
        window.setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        window.setTitle("NinjaConsole");
        window.setVisible(true);

        String workingDir = "/Users/abubics/Development/git_sandbox_mvpss/devices_ember/em35x-ezsp/build/smartmeter";
        String command = "./smartmeter -p /dev/cu.SLABtoUART -o 0";
        console.startProcess(workingDir, command);
    }
}
