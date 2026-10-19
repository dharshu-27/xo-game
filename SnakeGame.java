import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SnakeGame extends JPanel {
    private static final int CELL_SIZE = 24;
    private static final int COLUMNS = 25;
    private static final int ROWS = 25;
    private static final int BOARD_SIZE = CELL_SIZE * COLUMNS;
    private static final int INITIAL_DELAY = 140;

    private final Timer timer;
    private final Random random = new Random();
    private final List<Point> snake = new ArrayList<>();

    private Point food;
    private Direction direction = Direction.RIGHT;
    private boolean gameOver;
    private boolean won;
    private boolean paused = true;
    private boolean directionChanged;
    private int score;

    public SnakeGame() {
        setPreferredSize(new Dimension(BOARD_SIZE, BOARD_SIZE + 54));
        setBackground(new Color(18, 24, 32));
        setFocusable(true);

        bindKey("UP", "up", () -> changeDirection(Direction.UP));
        bindKey("DOWN", "down", () -> changeDirection(Direction.DOWN));
        bindKey("LEFT", "left", () -> changeDirection(Direction.LEFT));
        bindKey("RIGHT", "right", () -> changeDirection(Direction.RIGHT));
        bindKey("W", "w", () -> changeDirection(Direction.UP));
        bindKey("S", "s", () -> changeDirection(Direction.DOWN));
        bindKey("A", "a", () -> changeDirection(Direction.LEFT));
        bindKey("D", "d", () -> changeDirection(Direction.RIGHT));
        bindKey("SPACE", "pause", this::togglePause);
        bindKey("R", "restart", this::restartGame);

        restartGame();
        paused = true;
        timer = new Timer(INITIAL_DELAY, event -> advance());
        timer.start();
    }

    private void bindKey(String key, String name, Runnable action) {
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(key), name);
        getActionMap().put(name, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                action.run();
            }
        });
    }

    private void changeDirection(Direction next) {
        if (!directionChanged && !next.isOpposite(direction) && !gameOver) {
            direction = next;
            directionChanged = true;
            if (paused) {
                paused = false;
            }
        }
    }

    private void togglePause() {
        if (!gameOver) {
            paused = !paused;
            repaint();
        }
    }

    private void restartGame() {
        snake.clear();
        int startX = COLUMNS / 2;
        int startY = ROWS / 2;
        snake.add(new Point(startX, startY));
        snake.add(new Point(startX - 1, startY));
        snake.add(new Point(startX - 2, startY));
        direction = Direction.RIGHT;
        directionChanged = false;
        score = 0;
        gameOver = false;
        won = false;
        paused = false;
        placeFood();
        if (timer != null) {
            timer.setDelay(INITIAL_DELAY);
        }
        repaint();
    }

    private boolean placeFood() {
        if (snake.size() == COLUMNS * ROWS) {
            food = null;
            return false;
        }
        Point candidate;
        do {
            candidate = new Point(random.nextInt(COLUMNS), random.nextInt(ROWS));
        } while (snake.contains(candidate));
        food = candidate;
        return true;
    }

    private void advance() {
        if (paused || gameOver) {
            return;
        }

        Point head = snake.get(0);
        Point nextHead = new Point(head.x + direction.dx, head.y + direction.dy);
        boolean eating = nextHead.equals(food);

        if (nextHead.x < 0 || nextHead.x >= COLUMNS
                || nextHead.y < 0 || nextHead.y >= ROWS
                || hitsSnake(nextHead, eating)) {
            gameOver = true;
            repaint();
            return;
        }

        snake.add(0, nextHead);
        if (eating) {
            score += 10;
            if (!placeFood()) {
                won = true;
                gameOver = true;
            }
            timer.setDelay(Math.max(60, INITIAL_DELAY - score / 10 * 4));
        } else {
            snake.remove(snake.size() - 1);
        }
        directionChanged = false;
        repaint();
    }

    private boolean hitsSnake(Point point, boolean eating) {
        int bodyLength = eating ? snake.size() : snake.size() - 1;
        for (int i = 0; i < bodyLength; i++) {
            if (snake.get(i).equals(point)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g.setColor(new Color(28, 37, 48));
        g.fillRoundRect(0, 0, BOARD_SIZE, BOARD_SIZE, 14, 14);

        g.setColor(new Color(39, 50, 63));
        for (int x = 0; x <= BOARD_SIZE; x += CELL_SIZE) {
            g.drawLine(x, 0, x, BOARD_SIZE);
        }
        for (int y = 0; y <= BOARD_SIZE; y += CELL_SIZE) {
            g.drawLine(0, y, BOARD_SIZE, y);
        }

        if (food != null) {
            g.setColor(new Color(255, 91, 91));
            g.fillOval(food.x * CELL_SIZE + 4, food.y * CELL_SIZE + 4,
                    CELL_SIZE - 8, CELL_SIZE - 8);
        }

        for (int i = 0; i < snake.size(); i++) {
            Point segment = snake.get(i);
            g.setColor(i == 0 ? new Color(107, 235, 150) : new Color(56, 190, 111));
            g.fillRoundRect(segment.x * CELL_SIZE + 2, segment.y * CELL_SIZE + 2,
                    CELL_SIZE - 4, CELL_SIZE - 4, 8, 8);
        }

        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 18));
        g.drawString("Score: " + score, 14, BOARD_SIZE + 34);
        g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        g.setColor(new Color(185, 198, 211));
        g.drawString("Arrows/WASD: move    Space: pause    R: restart",
                115, BOARD_SIZE + 34);

        if (paused || gameOver) {
            g.setColor(new Color(0, 0, 0, 155));
            g.fillRoundRect(35, BOARD_SIZE / 2 - 58, BOARD_SIZE - 70, 116, 18, 18);
            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.BOLD, 30));
            String title = gameOver ? (won ? "You Win!" : "Game Over") : "Snake";
            drawCentered(g, title, BOARD_SIZE / 2);
            g.setFont(new Font("SansSerif", Font.PLAIN, 16));
            String subtitle = gameOver
                    ? "Score: " + score + "    Press R to play again"
                    : "Press an arrow key or WASD to start";
            drawCentered(g, subtitle, BOARD_SIZE / 2 + 31);
        }

        g.dispose();
    }

    private void drawCentered(Graphics2D g, String text, int y) {
        FontMetrics metrics = g.getFontMetrics();
        g.drawString(text, (BOARD_SIZE - metrics.stringWidth(text)) / 2, y);
    }

    private enum Direction {
        UP(0, -1), DOWN(0, 1), LEFT(-1, 0), RIGHT(1, 0);

        private final int dx;
        private final int dy;

        Direction(int dx, int dy) {
            this.dx = dx;
            this.dy = dy;
        }

        private boolean isOpposite(Direction other) {
            return dx + other.dx == 0 && dy + other.dy == 0;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Snake");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
            frame.add(new SnakeGame());
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
