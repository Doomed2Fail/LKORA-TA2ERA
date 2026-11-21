import javax.swing.JFrame;

/** La fenêtre contient le panneau qui dessine et anime le jeu. */
public class FenetreJeu extends JFrame {
    public FenetreJeu() {
        super("Lkora Ta2era");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setContentPane(new PanneauJeu());
        pack();
        setResizable(false);
        setLocationRelativeTo(null);
    }
}
