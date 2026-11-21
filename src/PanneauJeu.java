import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

/** Dessin, événements et règles du casse-briques. */
public class PanneauJeu extends JPanel {
    private static final int LARGEUR = 800, HAUTEUR = 650;
    private static final int BARRE_Y = 590, BARRE_H = 14, RAYON = 7;
    private final ArrayList<Balle> balles = new ArrayList<>();
    private final ArrayList<Brique> briques = new ArrayList<>();
    private final ArrayList<Bonus> bonus = new ArrayList<>();
    private final Random hasard = new Random();
    private final Timer timer;
    private int barreX = 350, barreLargeur = 100, niveau = 1, score = 0;
    private int tempsBarreLarge = 0;
    private boolean gauche, droite;
    // PRET : balle sur la barre ; JEU : animation ; PAUSE ; PERDU ; GAGNE.
    private String etat = "PRET";

    public PanneauJeu() {
        setPreferredSize(new Dimension(LARGEUR, HAUTEUR));
        setBackground(new Color(12, 18, 39));
        initialiserNiveau();
        touches();
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                barreX = Math.max(0, Math.min(LARGEUR - barreLargeur, e.getX() - barreLargeur / 2));
                repaint();
            }
            @Override public void mouseDragged(MouseEvent e) { mouseMoved(e); }
        });
        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { actionPrincipale(); }
        });
        timer = new Timer(16, e -> actualiser());
        timer.start();
    }

    private void touches() {
        InputMap im = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getActionMap();
        lier(im, am, "pressed LEFT", "gauche", () -> gauche = true);
        lier(im, am, "released LEFT", "stopGauche", () -> gauche = false);
        lier(im, am, "pressed RIGHT", "droite", () -> droite = true);
        lier(im, am, "released RIGHT", "stopDroite", () -> droite = false);
        lier(im, am, "pressed SPACE", "action", this::actionPrincipale);
        lier(im, am, "pressed P", "pause", () -> {
            if (etat.equals("JEU")) etat = "PAUSE";
            else if (etat.equals("PAUSE")) etat = "JEU";
            repaint();
        });
        lier(im, am, "pressed R", "recommencer", this::nouvellePartie);
    }

    private void lier(InputMap im, ActionMap am, String touche, String nom, Runnable action) {
        im.put(KeyStroke.getKeyStroke(touche), nom);
        am.put(nom, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { action.run(); }
        });
    }

    private void actionPrincipale() {
        if (etat.equals("PRET")) {
            balles.add(new Balle(barreX + barreLargeur / 2.0, BARRE_Y - RAYON - 1, 3, -5));
            etat = "JEU";
        } else if (etat.equals("PERDU") || etat.equals("GAGNE")) nouvellePartie();
        repaint();
    }

    private void nouvellePartie() {
        niveau = 1; score = 0;
        initialiserNiveau();
    }

    private void initialiserNiveau() {
        balles.clear(); briques.clear(); bonus.clear();
        barreLargeur = 100; tempsBarreLarge = 0; barreX = 350;
        gauche = false; droite = false;
        int lignes = niveau + 2;
        for (int ligne = 0; ligne < lignes; ligne++) {
            for (int col = 0; col < 10; col++) {
                // Un motif différent pour chaque niveau, sans briques impossibles à atteindre.
                if (niveau == 2 && ligne == 0 && (col == 0 || col == 9)) continue;
                if (niveau == 3 && ligne == 0 && (col < 2 || col > 7)) continue;
                int resistance = niveau >= 2 && ligne == 0 ? 2 : 1;
                briques.add(new Brique(45 + col * 72, 90 + ligne * 30, resistance, ligne));
            }
        }
        etat = "PRET";
        repaint();
    }

    private void actualiser() {
        if (!etat.equals("JEU")) return;
        if (gauche) barreX -= 8;
        if (droite) barreX += 8;
        barreX = Math.max(0, Math.min(LARGEUR - barreLargeur, barreX));
        if (tempsBarreLarge > 0 && --tempsBarreLarge == 0) {
            barreLargeur = 100;
            barreX = Math.min(barreX, LARGEUR - barreLargeur);
        }

        Iterator<Balle> it = balles.iterator();
        while (it.hasNext()) {
            Balle b = it.next();
            double ancienX = b.x, ancienY = b.y;
            b.x += b.dx; b.y += b.dy;
            if (b.x < RAYON) { b.x = RAYON; b.dx = Math.abs(b.dx); }
            if (b.x > LARGEUR - RAYON) { b.x = LARGEUR - RAYON; b.dx = -Math.abs(b.dx); }
            if (b.y < 55 + RAYON) { b.y = 55 + RAYON; b.dy = Math.abs(b.dy); }
            if (b.y - RAYON > HAUTEUR) {
                balles.clear();
                bonus.clear();
                etat = "PERDU";
                repaint();
                return;
            }

            Rectangle barre = new Rectangle(barreX, BARRE_Y, barreLargeur, BARRE_H);
            if (b.dy > 0 && ancienY + RAYON <= BARRE_Y && balleTouche(b, barre)) {
                double position = (b.x - (barreX + barreLargeur / 2.0)) / (barreLargeur / 2.0);
                b.dx = Math.max(-5, Math.min(5, position * 5));
                b.dy = -Math.sqrt(36 - b.dx * b.dx);
                b.y = BARRE_Y - RAYON;
            }
            Iterator<Brique> bi = briques.iterator();
            while (bi.hasNext()) {
                Brique br = bi.next();
                if (balleTouche(b, br.rect)) {
                    if (ancienX + RAYON <= br.rect.x || ancienX - RAYON >= br.rect.x + br.rect.width) b.dx = -b.dx;
                    else b.dy = -b.dy;
                    b.x = ancienX; b.y = ancienY;
                    br.resistance--;
                    if (br.resistance == 0) {
                        bi.remove(); score += 10;
                        if (hasard.nextInt(5) == 0) bonus.add(new Bonus(br.rect.x + br.rect.width / 2, br.rect.y, hasard.nextInt(2)));
                    }
                    break;
                }
            }
        }

        Iterator<Bonus> bo = bonus.iterator();
        while (bo.hasNext()) {
            Bonus cadeau = bo.next(); cadeau.y += 2.5;
            if (new Rectangle(barreX, BARRE_Y, barreLargeur, BARRE_H).intersects(cadeau.x - 12, cadeau.y - 12, 24, 24)) {
                appliquerBonus(cadeau.type); bo.remove();
            } else if (cadeau.y > HAUTEUR) bo.remove();
        }
        if (briques.isEmpty()) {
            if (niveau == 3) { etat = "GAGNE"; balles.clear(); }
            else { niveau++; initialiserNiveau(); }
        }
        repaint();
    }

    private boolean balleTouche(Balle b, Rectangle r) {
        return new Ellipse2D.Double(b.x - RAYON, b.y - RAYON, RAYON * 2, RAYON * 2).intersects(r);
    }

    private void appliquerBonus(int type) {
        if (type == 0 && !balles.isEmpty()) {
            ArrayList<Balle> nouvelles = new ArrayList<>();
            for (Balle b : balles) {
                if (balles.size() + nouvelles.size() >= 8) break;
                double dx = Math.abs(b.dx) < 1 ? 3 : -b.dx;
                nouvelles.add(new Balle(b.x, b.y, dx, b.dy));
            }
            balles.addAll(nouvelles);
        } else if (type == 1) {
            barreLargeur = 160; barreX = Math.min(barreX, LARGEUR - barreLargeur);
            tempsBarreLarge = 625; // Environ 10 secondes à 60 images par seconde.
        }
    }

    @Override protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D d = (Graphics2D) g.create();
        d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        d.setColor(new Color(24, 35, 65)); d.fillRect(0, 0, LARGEUR, 55);
        d.setFont(new Font("SansSerif", Font.BOLD, 18)); d.setColor(Color.WHITE);
        d.drawString("LKORA TA2ERA", 20, 34);
        d.setFont(new Font("SansSerif", Font.PLAIN, 16));
        d.drawString("Niveau " + niveau + "/3", 285, 34);
        d.drawString("Score : " + score, 420, 34);
        Color[] couleurs = {new Color(255, 105, 110), new Color(255, 176, 84), new Color(255, 216, 104), new Color(73, 203, 180), new Color(105, 166, 255)};
        for (Brique br : briques) {
            d.setColor(couleurs[br.ligne % couleurs.length]);
            d.fillRoundRect(br.rect.x, br.rect.y, br.rect.width, br.rect.height, 8, 8);
            if (br.resistance > 1) { d.setColor(Color.WHITE); d.drawString("2", br.rect.x + 29, br.rect.y + 17); }
        }
        d.setColor(new Color(100, 222, 237)); d.fillRoundRect(barreX, BARRE_Y, barreLargeur, BARRE_H, 14, 14);
        d.setColor(Color.WHITE);
        for (Balle b : balles) d.fillOval((int)b.x - RAYON, (int)b.y - RAYON, RAYON * 2, RAYON * 2);
        if (etat.equals("PRET")) d.fillOval(barreX + barreLargeur / 2 - RAYON, BARRE_Y - 2 * RAYON - 1, RAYON * 2, RAYON * 2);
        for (Bonus cadeau : bonus) {
            d.setColor(cadeau.type == 0 ? new Color(186, 131, 255) : cadeau.type == 1 ? new Color(82, 214, 193) : new Color(255, 112, 130));
            d.fillOval((int)cadeau.x - 13, (int)cadeau.y - 13, 26, 26);
            d.setColor(Color.WHITE); d.setFont(new Font("SansSerif", Font.BOLD, 11));
            d.drawString(cadeau.type == 0 ? "x2" : cadeau.type == 1 ? "↔" : "+1", (int)cadeau.x - 8, (int)cadeau.y + 4);
        }
        d.setColor(new Color(165, 180, 208)); d.setFont(new Font("SansSerif", Font.PLAIN, 12));
        d.drawString("Souris ou flèches : déplacer   •   Espace / clic : lancer   •   P : pause   •   R : recommencer", 115, 630);
        if (!etat.equals("JEU")) {
            String titre = etat.equals("PRET") ? "Niveau " + niveau : etat.equals("PAUSE") ? "Pause" : etat.equals("GAGNE") ? "Bravo, tu as gagné !" : "Partie terminée";
            String aide = etat.equals("PRET") ? "Espace ou clic pour lancer la balle" : etat.equals("PAUSE") ? "Appuie sur P pour reprendre" : "Espace, clic ou R pour rejouer";
            d.setColor(new Color(8, 13, 30, 220)); d.fillRoundRect(205, 310, 390, 95, 18, 18);
            d.setColor(Color.WHITE); d.setFont(new Font("SansSerif", Font.BOLD, 25)); centrer(d, titre, 348);
            d.setColor(new Color(180, 195, 220)); d.setFont(new Font("SansSerif", Font.PLAIN, 15)); centrer(d, aide, 380);
        }
        d.dispose();
    }

    private void centrer(Graphics2D g, String texte, int y) { g.drawString(texte, (LARGEUR - g.getFontMetrics().stringWidth(texte)) / 2, y); }

    private static class Balle {
        double x, y, dx, dy;
        Balle(double x, double y, double dx, double dy) { this.x = x; this.y = y; this.dx = dx; this.dy = dy; }
    }
    private static class Brique {
        Rectangle rect; int resistance, ligne;
        Brique(int x, int y, int resistance, int ligne) { rect = new Rectangle(x, y, 66, 24); this.resistance = resistance; this.ligne = ligne; }
    }
    private static class Bonus {
        double x, y; int type;
        Bonus(double x, double y, int type) { this.x = x; this.y = y; this.type = type; }
    }
}
