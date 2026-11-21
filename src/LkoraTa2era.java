import javax.swing.SwingUtilities;

/** Point de départ du jeu. */
public class LkoraTa2era {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new FenetreJeu().setVisible(true));
    }
}
