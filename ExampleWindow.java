import javax.swing.JFrame;

import NinjaConsole.*;

public class ExampleWindow
{
    public static void main(String [] args)
    {
        JFrame window = new JFrame();
        NinjaConsole console = new NinjaConsole();
        console.setInteractive(true);
        window.setContentPane(console);
        window.setSize(640, 480);
        window.setLocation(320, 240);
        window.setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        window.setTitle("NinjaConsole");
        window.setVisible(true);

        String workingDir = ".";
        String command = "pwd";
        console.startProcess(workingDir, command);
    }
}