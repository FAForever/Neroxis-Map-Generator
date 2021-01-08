package neroxis.generator;

import neroxis.map.SCMap;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

//Yes, that's swing. Get over it.
public strictfp class Visualizer extends JPanel {

    private final JFrame frame;
    private final ArrayList<SCMap> maps;

    public Visualizer() {
        frame = new JFrame("NeroxisGen");
        maps = new ArrayList<>();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setUndecorated(true);
        frame.setResizable(false);
        frame.add(this);
        frame.setVisible(true);
    }

    public void addMap(SCMap map) {
        maps.add(map);
        frame.setBounds(0, 0, maps.size() * (map.getSize() / 2 + 16), map.getSize() / 2 + 16);
        frame.repaint();
    }

    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics;
        for (int i = 0; i < maps.size(); i++) {
            g.drawImage(maps.get(i).getPreview(), i * (maps.get(i).getSize() / 2 + 16) + 8, 8, null);
            g.setColor(Color.BLUE);
        }
    }
}
