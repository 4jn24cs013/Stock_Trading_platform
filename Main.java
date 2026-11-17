import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.geom.*;
import java.io.*;
import java.text.DecimalFormat;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/*
 * NEXUS - Java Swing Stock Trading Platform
 * Task 2: Market data, buy/sell, portfolio, performance and file storage.
 * Prices are simulated; this is a paper-trading application.
 */
public class Main extends JFrame {
    static final Color BG = new Color(10, 16, 31);
    static final Color SIDE = new Color(13, 23, 43);
    static final Color PANEL = new Color(22, 34, 58);
    static final Color PANEL2 = new Color(30, 46, 76);
    static final Color BLUE = new Color(83, 145, 255);
    static final Color CYAN = new Color(45, 205, 225);
    static final Color PURPLE = new Color(167, 119, 255);
    static final Color GREEN = new Color(38, 220, 160);
    static final Color RED = new Color(255, 105, 125);
    static final Color TEXT = new Color(245, 248, 255);
    static final Color MUTED = new Color(174, 190, 216);
    static final DecimalFormat DF = new DecimalFormat("#,##0.00");
    static final String DATA_FILE = "nexus-data.properties";

    final StockMarket market = new StockMarket();
    final User user = new User("Demo Investor", 100000.00);
    final java.util.List<Double> performance = new ArrayList<>();
    final java.util.List<String> transactions = new ArrayList<>();

    JPanel content;
    CardLayout cardLayout;
    JPanel pages;
    JButton[] navButtons;
    JLabel cashValue, holdingsValue, totalValue, pnlValue, marketStatus;
    DefaultTableModel marketModel;
    JTable marketTable;
    PerformanceChart chart;

    public Main() {
        super("NEXUS | Stock Trading Platform");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1450, 850);
        setMinimumSize(new Dimension(1100, 700));
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG);

        market.loadDemoStocks();
        loadData();
        if (performance.isEmpty()) performance.add(accountValue());

        buildUI();
        refreshAll();

        new javax.swing.Timer(12000, e -> {
            market.simulatePriceChanges();
            performance.add(accountValue());
            if (performance.size() > 40) performance.remove(0);
            refreshAll();
            saveData();
        }).start();
    }

    private void buildUI() {
        setLayout(new BorderLayout());
        add(buildSidebar(), BorderLayout.WEST);

        content = new JPanel(new BorderLayout());
        content.setBackground(BG);
        content.setBorder(new EmptyBorder(27, 30, 24, 30));

        cardLayout = new CardLayout();
        pages = new JPanel(cardLayout);
        pages.setOpaque(false);
        pages.add(buildOverview(), "Overview");
        pages.add(buildMarketPage(), "Market");
        pages.add(buildPortfolioPage(), "My Portfolio");
        pages.add(buildTransactionsPage(), "Transactions");

        content.add(pages, BorderLayout.CENTER);
        add(content, BorderLayout.CENTER);
        showPage("Overview");
    }

    private JPanel buildSidebar() {
        JPanel side = new JPanel(new BorderLayout());
        side.setPreferredSize(new Dimension(245, 0));
        side.setBackground(SIDE);
        side.setBorder(new EmptyBorder(28, 17, 22, 17));

        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));

        JLabel logo = new JLabel("◆  NEXUS");
        logo.setForeground(TEXT);
        logo.setFont(new Font("Segoe UI", Font.BOLD, 27));

        JLabel sub = new JLabel("     SMART INVESTING");
        sub.setForeground(CYAN);
        sub.setFont(new Font("Segoe UI", Font.BOLD, 10));

        top.add(logo);
        top.add(Box.createVerticalStrut(5));
        top.add(sub);
        top.add(Box.createVerticalStrut(48));

        JLabel section = new JLabel("  MAIN MENU");
        section.setForeground(MUTED);
        section.setFont(new Font("Segoe UI", Font.BOLD, 11));
        top.add(section);
        top.add(Box.createVerticalStrut(14));

        String[] names = {
            "Overview", "Market", "My Portfolio", "Transactions"
        };

        String[] icons = {"⌂", "▤", "▣", "◷"};
        navButtons = new JButton[names.length];

        for (int i = 0; i < names.length; i++) {
            final String name = names[i];

            JButton b = new ColorButton(icons[i] + "    " + name,new Color(34,51,82));
            b.setHorizontalAlignment(SwingConstants.LEFT);
            b.setFont(new Font("Segoe UI", Font.BOLD, 14));
            b.setForeground(TEXT);
            b.setBackground(new Color(34, 51, 82));
            b.setBorder(new EmptyBorder(15, 16, 15, 10));
            b.setFocusPainted(false);
            b.setOpaque(false);
            b.setContentAreaFilled(false);
            b.setBorderPainted(false);
            b.setMaximumSize(new Dimension(210, 51));
            b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            b.addActionListener(e -> showPage(name));

            b.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseEntered(java.awt.event.MouseEvent e) {
                    if (!name.equals(getCurrentPage())) {
                        b.setBackground(new Color(48, 70, 108));
                    }
                }

                public void mouseExited(java.awt.event.MouseEvent e) {
                    if (!name.equals(getCurrentPage())) {
                        b.setBackground(new Color(34, 51, 82));
                    }
                }
            });

            navButtons[i] = b;
            top.add(b);
            top.add(Box.createVerticalStrut(9));
        }

        side.add(top, BorderLayout.NORTH);

        JPanel profile = new RoundedPanel(PANEL2, 18);
        profile.setLayout(new BorderLayout(10, 0));
        profile.setBorder(new EmptyBorder(13, 12, 13, 12));

        JLabel avatar = new JLabel("U", SwingConstants.CENTER);
        avatar.setOpaque(true);
        avatar.setBackground(BLUE);
        avatar.setForeground(Color.WHITE);
        avatar.setFont(new Font("Segoe UI", Font.BOLD, 19));
        avatar.setPreferredSize(new Dimension(40, 40));

        JPanel labels = new JPanel();
        labels.setOpaque(false);
        labels.setLayout(new BoxLayout(labels, BoxLayout.Y_AXIS));

        JLabel name = new JLabel(user.getName());
        name.setForeground(TEXT);
        name.setFont(new Font("Segoe UI", Font.BOLD, 12));

        JLabel type = new JLabel("Paper trading account");
        type.setForeground(MUTED);
        type.setFont(new Font("Segoe UI", Font.PLAIN, 10));

        labels.add(name);
        labels.add(Box.createVerticalStrut(4));
        labels.add(type);

        profile.add(avatar, BorderLayout.WEST);
        profile.add(labels, BorderLayout.CENTER);
        side.add(profile, BorderLayout.SOUTH);

        return side;
    }

    private String getCurrentPage() {
        for (int i = 0; i < navButtons.length; i++) {
            if (navButtons[i].getBackground().equals(
                    new Color(63, 105, 170))) {
                return new String[]{
                    "Overview", "Market", "My Portfolio", "Transactions"
                }[i];
            }
        }
        return "";
    }

    private void showPage(String name) {
        String[] names = {
            "Overview", "Market", "My Portfolio", "Transactions"
        };

        for (int i = 0; i < names.length; i++) {
            boolean active = names[i].equals(name);

            navButtons[i].setBackground(
                active ? new Color(63, 105, 170)
                       : new Color(34, 51, 82)
            );

            navButtons[i].setForeground(
                active ? Color.WHITE
                       : new Color(215, 226, 245)
            );
        }

        cardLayout.show(pages, name);
        refreshAll();
    }

    private JPanel buildOverview() {
        JPanel root = new JPanel(new BorderLayout(0, 22));
        root.setOpaque(false);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));

        JLabel heading = new JLabel("Good day, Investor!");
        heading.setForeground(TEXT);
        heading.setFont(new Font("Segoe UI", Font.BOLD, 29));

        JLabel subtitle = new JLabel(
            "Monitor your investments and make your next move."
        );
        subtitle.setForeground(MUTED);
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        titles.add(heading);
        titles.add(Box.createVerticalStrut(6));
        titles.add(subtitle);

        JButton refresh = brightButton("↻  Refresh", BLUE);
        refresh.addActionListener(e -> refreshAll());

        header.add(titles, BorderLayout.WEST);
        header.add(refresh, BorderLayout.EAST);

        JPanel cards = new JPanel(new GridLayout(1, 4, 15, 0));
        cards.setOpaque(false);

        cashValue = new JLabel();
        holdingsValue = new JLabel();
        totalValue = new JLabel();
        pnlValue = new JLabel();

        cards.add(statCard(
            "AVAILABLE CASH", cashValue,
            "Ready to invest", BLUE, "₹"
        ));

        cards.add(statCard(
            "HOLDINGS VALUE", holdingsValue,
            "Current investments", GREEN, "▣"
        ));

        cards.add(statCard(
            "TOTAL ASSETS", totalValue,
            "Cash + holdings", CYAN, "◆"
        ));

        cards.add(statCard(
            "PROFIT / LOSS", pnlValue,
            "Since starting balance", PURPLE, "↗"
        ));

        JPanel middle = new JPanel(new GridLayout(1, 2, 17, 0));
        middle.setOpaque(false);

        JPanel marketPanel = makePanel(
            "MARKET WATCH",
            "Popular stocks • simulated market prices"
        );

        marketTable = makeMarketTable();

        JScrollPane marketScroll = new JScrollPane(marketTable);
        styleScroll(marketScroll);
        marketPanel.add(marketScroll, BorderLayout.CENTER);

        JPanel right = new JPanel(new GridLayout(2, 1, 0, 15));
        right.setOpaque(false);

        JPanel chartPanel = makePanel(
            "PORTFOLIO PERFORMANCE",
            "Account value over time"
        );

        chart = new PerformanceChart();
        chartPanel.add(chart, BorderLayout.CENTER);

        JPanel tradePanel = makePanel(
            "QUICK TRADE",
            "Place a paper-trading order"
        );

        JPanel actions = new JPanel(new GridLayout(1, 2, 12, 0));
        actions.setOpaque(false);

        JButton buy = brightButton("＋  BUY STOCK", GREEN);
        JButton sell = brightButton("－  SELL STOCK", RED);

        buy.addActionListener(e -> trade(true));
        sell.addActionListener(e -> trade(false));

        actions.add(buy);
        actions.add(sell);
        tradePanel.add(actions, BorderLayout.CENTER);

        right.add(chartPanel);
        right.add(tradePanel);

        middle.add(marketPanel);
        middle.add(right);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);

        marketStatus = new JLabel("●  MARKET SIMULATION ACTIVE");
        marketStatus.setForeground(GREEN);
        marketStatus.setFont(new Font("Segoe UI", Font.BOLD, 10));

        JLabel note = new JLabel(
            "NEXUS PAPER TRADING  •  NOT REAL-MONEY TRADING"
        );
        note.setForeground(MUTED);
        note.setFont(new Font("Segoe UI", Font.BOLD, 10));

        footer.add(marketStatus, BorderLayout.WEST);
        footer.add(note, BorderLayout.EAST);

        JPanel center = new JPanel(new BorderLayout(0, 20));
        center.setOpaque(false);
        center.add(cards, BorderLayout.NORTH);
        center.add(middle, BorderLayout.CENTER);

        root.add(header, BorderLayout.NORTH);
        root.add(center, BorderLayout.CENTER);
        root.add(footer, BorderLayout.SOUTH);

        return root;
    }
    private JPanel buildMarketPage() {
        JPanel p = makePanel(
            "STOCK MARKET",
            "Browse available stocks and current simulated prices"
        );

        JTable table = makeMarketTable();
        JScrollPane scroll = new JScrollPane(table);
        styleScroll(scroll);
        p.add(scroll, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.setOpaque(false);

        JButton buy = brightButton("＋ BUY STOCK", GREEN);
        JButton sell = brightButton("－ SELL STOCK", RED);

        buy.addActionListener(e -> trade(true));
        sell.addActionListener(e -> trade(false));

        buttons.add(buy);
        buttons.add(sell);
        p.add(buttons, BorderLayout.SOUTH);

        return p;
    }

    private JPanel buildPortfolioPage() {
        JPanel p = makePanel(
            "MY PORTFOLIO",
            "Your owned shares and current investment value"
        );

        String[] cols = {
            "SYMBOL", "COMPANY", "SHARES",
            "MARKET PRICE", "TOTAL VALUE"
        };

        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        JTable table = makeTable(model);
        JScrollPane scroll = new JScrollPane(table);
        styleScroll(scroll);
        p.add(scroll, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.setOpaque(false);

        JButton sell = brightButton("－ Sell shares", RED);
        sell.addActionListener(e -> trade(false));

        bottom.add(sell);
        p.add(bottom, BorderLayout.SOUTH);

        return p;
    }

    private JPanel buildTransactionsPage() {
        JPanel p = makePanel(
            "TRANSACTION HISTORY",
            "A record of every completed buy and sell order"
        );

        String[] cols = {
            "TYPE", "STOCK", "QUANTITY",
            "PRICE", "TOTAL", "TIME"
        };

        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        JTable table = makeTable(model);
        JScrollPane scroll = new JScrollPane(table);
        styleScroll(scroll);
        p.add(scroll, BorderLayout.CENTER);

        return p;
    }

    private JPanel statCard(
            String title,
            JLabel value,
            String caption,
            Color accent,
            String symbol) {

        JPanel card = new RoundedPanel(PANEL, 20);
        card.setLayout(new BorderLayout(0, 13));
        card.setBorder(new EmptyBorder(17, 17, 17, 17));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);

        JLabel t = new JLabel(title);
        t.setForeground(MUTED);
        t.setFont(new Font("Segoe UI", Font.BOLD, 10));

        JLabel icon = new JLabel(symbol, SwingConstants.CENTER);
        icon.setOpaque(true);
        icon.setBackground(new Color(
            accent.getRed(),
            accent.getGreen(),
            accent.getBlue(),
            38
        ));
        icon.setForeground(accent);
        icon.setFont(new Font("Segoe UI", Font.BOLD, 17));
        icon.setPreferredSize(new Dimension(35, 35));

        top.add(t, BorderLayout.WEST);
        top.add(icon, BorderLayout.EAST);

        value.setForeground(accent);
        value.setFont(new Font("Segoe UI", Font.BOLD, 20));

        JLabel desc = new JLabel(caption);
        desc.setForeground(MUTED);
        desc.setFont(new Font("Segoe UI", Font.PLAIN, 11));

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));

        text.add(value);
        text.add(Box.createVerticalStrut(6));
        text.add(desc);

        card.add(top, BorderLayout.NORTH);
        card.add(text, BorderLayout.CENTER);

        return card;
    }

    private JPanel makePanel(String title, String subtitle) {
        JPanel p = new RoundedPanel(PANEL, 20);
        p.setLayout(new BorderLayout(0, 15));
        p.setBorder(new EmptyBorder(18, 18, 18, 18));

        JPanel labels = new JPanel();
        labels.setOpaque(false);
        labels.setLayout(new BoxLayout(labels, BoxLayout.Y_AXIS));

        JLabel h = new JLabel(title);
        h.setForeground(TEXT);
        h.setFont(new Font("Segoe UI", Font.BOLD, 14));

        JLabel s = new JLabel(subtitle);
        s.setForeground(MUTED);
        s.setFont(new Font("Segoe UI", Font.PLAIN, 11));

        labels.add(h);
        labels.add(Box.createVerticalStrut(5));
        labels.add(s);

        p.add(labels, BorderLayout.NORTH);

        return p;
    }

    private JTable makeMarketTable() {
        if (marketModel == null) {
            String[] cols = {
                "SYMBOL", "COMPANY", "PRICE (₹)", "CHANGE"
            };

            marketModel = new DefaultTableModel(cols, 0) {
                public boolean isCellEditable(int r, int c) {
                    return false;
                }
            };
        }

        JTable t = makeTable(marketModel);

        t.getColumnModel().getColumn(3).setCellRenderer(
            new DefaultTableCellRenderer() {
                public Component getTableCellRendererComponent(
                        JTable table,
                        Object value,
                        boolean selected,
                        boolean focus,
                        int row,
                        int col) {

                    JLabel label = (JLabel)
                        super.getTableCellRendererComponent(
                            table, value, selected, focus, row, col
                        );

                    String s = String.valueOf(value);

                    label.setForeground(
                        s.startsWith("-") ? RED : GREEN
                    );

                    label.setFont(
                        new Font("Segoe UI", Font.BOLD, 11)
                    );

                    label.setHorizontalAlignment(
                        SwingConstants.CENTER
                    );

                    return label;
                }
            }
        );

        return t;
    }

    private JTable makeTable(DefaultTableModel model) {
        JTable t = new JTable(model);

        t.setRowHeight(39);
        t.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        t.setForeground(TEXT);
        t.setBackground(PANEL);
        t.setGridColor(new Color(48, 65, 94));
        t.setShowVerticalLines(false);
        t.setSelectionBackground(new Color(57, 88, 140));
        t.setSelectionForeground(Color.WHITE);

        JTableHeader h = t.getTableHeader();

        h.setBackground(new Color(43, 62, 96));
        h.setForeground(new Color(235, 243, 255));
        h.setFont(new Font("Segoe UI", Font.BOLD, 10));
        h.setPreferredSize(new Dimension(0, 36));
        h.setReorderingAllowed(false);

        return t;
    }

    private void styleScroll(JScrollPane s) {
        s.setBorder(new LineBorder(new Color(54, 73, 105)));
        s.getViewport().setBackground(PANEL);
        s.getVerticalScrollBar().setBackground(PANEL);
    }

    private JButton brightButton(String text, Color color) {
    JButton b = new JButton(text) {
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();

            g2.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
            );

            g2.setColor(getBackground());
            g2.fillRoundRect(
                0, 0, getWidth(), getHeight(), 15, 15
            );

            g2.dispose();
            super.paintComponent(g);
        }
    };

    b.setFont(new Font("Segoe UI", Font.BOLD, 13));
    b.setForeground(Color.WHITE);
    b.setBackground(color);
    b.setOpaque(false);
    b.setContentAreaFilled(false);
    b.setBorderPainted(false);
    b.setFocusPainted(false);
    b.setBorder(new EmptyBorder(13, 18, 13, 18));
    b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

    b.addMouseListener(new java.awt.event.MouseAdapter() {
        public void mouseEntered(java.awt.event.MouseEvent e) {
            b.setBackground(color.brighter());
            b.repaint();
        }

        public void mouseExited(java.awt.event.MouseEvent e) {
            b.setBackground(color);
            b.repaint();
        }
    });

    return b;
}
    private void trade(boolean buy) {
        String[] symbols = market.getSymbols();

        JComboBox<String> stockBox = new JComboBox<>(symbols);
        JTextField qtyField = new JTextField("1", 8);

        JPanel form = new JPanel(new GridLayout(0, 1, 6, 8));

        form.add(new JLabel("Select stock:"));
        form.add(stockBox);
        form.add(new JLabel("Quantity:"));
        form.add(qtyField);

        String action = buy ? "BUY" : "SELL";

        int result = JOptionPane.showConfirmDialog(
            this,
            form,
            action + " STOCK",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
        );

        if (result != JOptionPane.OK_OPTION) return;

        try {
            String symbol = (String) stockBox.getSelectedItem();
            int qty = Integer.parseInt(qtyField.getText().trim());

            if (qty <= 0) {
                throw new NumberFormatException(
                    "Quantity must be positive"
                );
            }

            Stock stock = market.getStock(symbol);
            double total = stock.getPrice() * qty;

            if (buy) {
                if (total > user.getCash()) {
                    JOptionPane.showMessageDialog(
                        this,
                        "Insufficient cash balance."
                    );
                    return;
                }

                user.buy(symbol, qty, total);

            } else {
                if (!user.getPortfolio().sell(symbol, qty)) {
                    JOptionPane.showMessageDialog(
                        this,
                        "You do not own enough shares."
                    );
                    return;
                }

                user.addCash(total);
            }

            Transaction tx = new Transaction(
                action,
                symbol,
                qty,
                stock.getPrice(),
                total
            );

            transactions.add(tx.toStorageString());
            performance.add(accountValue());

            saveData();
            refreshAll();

            JOptionPane.showMessageDialog(
                this,
                action + " order completed successfully!\n"
                    + qty + " shares of " + symbol,
                "NEXUS Trade",
                JOptionPane.INFORMATION_MESSAGE
            );

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(
                this,
                "Enter a valid positive whole number."
            );
        }
    }

    // Total account value includes cash and current share value.
    private double accountValue() {
        return user.getCash()
            + user.getPortfolio().getHoldingsValue(market);
    }

    private void refreshAll() {
        double holdings =
            user.getPortfolio().getHoldingsValue(market);

        double total = accountValue();
        double pnl = total - 100000.0;

        if (cashValue != null) {
            cashValue.setText("₹ " + DF.format(user.getCash()));
        }

        if (holdingsValue != null) {
            holdingsValue.setText("₹ " + DF.format(holdings));
        }

        if (totalValue != null) {
            totalValue.setText("₹ " + DF.format(total));
        }

        if (pnlValue != null) {
            pnlValue.setText(
                (pnl >= 0 ? "+" : "-")
                + "₹ " + DF.format(Math.abs(pnl))
            );

            pnlValue.setForeground(pnl >= 0 ? GREEN : RED);
        }

        if (marketStatus != null) {
            marketStatus.setText(
                "●  MARKET SIMULATION ACTIVE  |  "
                + LocalTime.now().format(
                    DateTimeFormatter.ofPattern("HH:mm:ss")
                )
            );
        }

        if (marketModel != null) {
            marketModel.setRowCount(0);

            for (Stock s : market.getAllStocks()) {
                marketModel.addRow(new Object[]{
                    s.getSymbol(),
                    s.getCompany(),
                    "₹ " + DF.format(s.getPrice()),
                    String.format("%+.2f%%", s.getChange())
                });
            }
        }

        if (pages != null) {
            updatePortfolioTable();
            updateTransactionsTable();

            if (chart != null) {
                chart.repaint();
            }

            pages.revalidate();
            pages.repaint();
        }
    }

    private void updatePortfolioTable() {
        JPanel p = (JPanel) pages.getComponent(2);
        JTable t = findTable(p);

        if (t == null) return;

        DefaultTableModel m = (DefaultTableModel) t.getModel();
        m.setRowCount(0);

        for (Map.Entry<String, Integer> e :
                user.getPortfolio().getShares().entrySet()) {

            Stock s = market.getStock(e.getKey());
            int q = e.getValue();

            m.addRow(new Object[]{
                s.getSymbol(),
                s.getCompany(),
                q,
                "₹ " + DF.format(s.getPrice()),
                "₹ " + DF.format(s.getPrice() * q)
            });
        }

        if (m.getRowCount() == 0) {
            m.addRow(new Object[]{
                "—", "No shares owned yet", "—", "—", "—"
            });
        }
    }

    private void updateTransactionsTable() {
        JPanel p = (JPanel) pages.getComponent(3);
        JTable t = findTable(p);

        if (t == null) return;

        DefaultTableModel m = (DefaultTableModel) t.getModel();
        m.setRowCount(0);

        for (int i = transactions.size() - 1; i >= 0; i--) {
            String[] a = transactions.get(i).split("\\|", -1);

            if (a.length == 6) {
                m.addRow(a);
            }
        }

        if (m.getRowCount() == 0) {
            m.addRow(new Object[]{
                "—", "No transactions yet",
                "—", "—", "—", "—"
            });
        }
    }

    private JTable findTable(Container c) {
        for (Component comp : c.getComponents()) {

            if (comp instanceof JTable) {
                return (JTable) comp;
            }

            if (comp instanceof JScrollPane) {
                Component v = ((JScrollPane) comp)
                    .getViewport().getView();

                if (v instanceof JTable) {
                    return (JTable) v;
                }
            }

            if (comp instanceof Container) {
                JTable found = findTable((Container) comp);

                if (found != null) {
                    return found;
                }
            }
        }

        return null;
    }
    private void saveData() {
        Properties p = new Properties();

        p.setProperty(
            "cash",
            String.valueOf(user.getCash())
        );

        for (Map.Entry<String, Integer> e :
                user.getPortfolio().getShares().entrySet()) {

            p.setProperty(
                "holding." + e.getKey(),
                String.valueOf(e.getValue())
            );
        }

        p.setProperty(
            "transaction.count",
            String.valueOf(transactions.size())
        );

        for (int i = 0; i < transactions.size(); i++) {
            p.setProperty(
                "transaction." + i,
                transactions.get(i)
            );
        }

        p.setProperty(
            "history.count",
            String.valueOf(performance.size())
        );

        for (int i = 0; i < performance.size(); i++) {
            p.setProperty(
                "history." + i,
                String.valueOf(performance.get(i))
            );
        }

        try (FileOutputStream out =
                new FileOutputStream(DATA_FILE)) {

            p.store(out, "NEXUS Stock Trading Platform data");

        } catch (IOException ex) {
            System.err.println(
                "Could not save data: " + ex.getMessage()
            );
        }
    }

    private void loadData() {
        File f = new File(DATA_FILE);

        if (!f.exists()) return;

        Properties p = new Properties();

        try (FileInputStream in = new FileInputStream(f)) {
            p.load(in);

            user.setCash(
                Double.parseDouble(
                    p.getProperty("cash", "100000")
                )
            );

            for (String symbol : market.getSymbols()) {
                int qty = Integer.parseInt(
                    p.getProperty("holding." + symbol, "0")
                );

                if (qty > 0) {
                    user.getPortfolio().addShares(symbol, qty);
                }
            }

            int tc = Integer.parseInt(
                p.getProperty("transaction.count", "0")
            );

            for (int i = 0; i < tc; i++) {
                String tx = p.getProperty("transaction." + i);

                if (tx != null) {
                    transactions.add(tx);
                }
            }

            int hc = Integer.parseInt(
                p.getProperty("history.count", "0")
            );

            for (int i = 0; i < hc; i++) {
                String value = p.getProperty("history." + i);

                if (value != null) {
                    performance.add(Double.parseDouble(value));
                }
            }

        } catch (Exception ex) {
            System.err.println(
                "Could not load saved data: " + ex.getMessage()
            );
        }
    }

    private class PerformanceChart extends JPanel {

        PerformanceChart() {
            setOpaque(false);
            setPreferredSize(new Dimension(360, 190));
        }

        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);

            Graphics2D g = (Graphics2D) graphics.create();

            g.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
            );

            int w = getWidth();
            int h = getHeight();

            g.setColor(new Color(64, 82, 115));

            for (int i = 0; i < 4; i++) {
                int y = 15 + i * (h - 45) / 3;
                g.drawLine(8, y, w - 8, y);
            }

            if (performance.size() < 2) {
                g.setColor(MUTED);
                g.drawString(
                    "Portfolio performance will appear here.",
                    15,
                    h / 2
                );

                g.dispose();
                return;
            }

            double min = Collections.min(performance);
            double max = Collections.max(performance);

            if (max - min < 1) {
                max = min + 100;
            }

            int n = performance.size();

            int[] xs = new int[n];
            int[] ys = new int[n];

            for (int i = 0; i < n; i++) {
                xs[i] = 10 + i * (w - 25) / Math.max(1, n - 1);

                ys[i] = h - 25 - (int) (
                    (performance.get(i) - min)
                    / (max - min) * (h - 55)
                );
            }

            Path2D line = new Path2D.Double();

            line.moveTo(xs[0], ys[0]);

            for (int i = 1; i < n; i++) {
                line.lineTo(xs[i], ys[i]);
            }

            Path2D area = new Path2D.Double(line);

            area.lineTo(xs[n - 1], h - 20);
            area.lineTo(xs[0], h - 20);
            area.closePath();

            g.setPaint(new GradientPaint(
                0, 0,
                new Color(38, 220, 160, 90),
                0, h,
                new Color(38, 220, 160, 0)
            ));

            g.fill(area);

            g.setColor(GREEN);

            g.setStroke(new BasicStroke(
                3f,
                BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND
            ));

            g.draw(line);

            g.setColor(Color.WHITE);

            g.fillOval(
                xs[n - 1] - 4,
                ys[n - 1] - 4,
                8,
                8
            );

            g.setColor(MUTED);
            g.setFont(new Font("Segoe UI", Font.PLAIN, 10));

            g.drawString(
                "₹ " + DF.format(performance.get(n - 1)),
                10,
                h - 4
            );

            g.dispose();
        }
    }

    // Object-oriented programming model classes

    static class Stock {

        private final String symbol;
        private final String company;

        private double price;
        private double change;

        Stock(String symbol, String company, double price) {
            this.symbol = symbol;
            this.company = company;
            this.price = price;
        }

        String getSymbol() {
            return symbol;
        }

        String getCompany() {
            return company;
        }

        double getPrice() {
            return price;
        }

        double getChange() {
            return change;
        }

        void updatePrice(double percent) {
            change = percent;

            price = Math.max(
                1,
                price * (1 + percent / 100.0)
            );
        }
    }

    static class StockMarket {

        private final LinkedHashMap<String, Stock> stocks =
            new LinkedHashMap<>();

        void loadDemoStocks() {
            add(new Stock(
                "TCS",
                "Tata Consultancy Services",
                3550.72
            ));

            add(new Stock(
                "INFY",
                "Infosys Limited",
                1836.41
            ));

            add(new Stock(
                "RELIANCE",
                "Reliance Industries",
                2943.32
            ));

            add(new Stock(
                "HDFCBANK",
                "HDFC Bank",
                1704.81
            ));

            add(new Stock(
                "WIPRO",
                "Wipro Limited",
                531.02
            ));

            add(new Stock(
                "ITC",
                "ITC Limited",
                463.27
            ));

            add(new Stock(
                "ICICIBANK",
                "ICICI Bank",
                1290.05
            ));

            add(new Stock(
                "BHARTIARTL",
                "Bharti Airtel",
                1587.41
            ));
        }

        void add(Stock s) {
            stocks.put(s.getSymbol(), s);
        }

        Stock getStock(String symbol) {
            return stocks.get(symbol);
        }

        Collection<Stock> getAllStocks() {
            return stocks.values();
        }

        String[] getSymbols() {
            return stocks.keySet().toArray(new String[0]);
        }

        void simulatePriceChanges() {
            Random r = new Random();

            for (Stock s : stocks.values()) {
                s.updatePrice(
                    (r.nextDouble() - 0.48) * 1.2
                );
            }
        }
    }

    static class Portfolio {

        private final LinkedHashMap<String, Integer> shares =
            new LinkedHashMap<>();

        void addShares(String symbol, int quantity) {
            shares.put(
                symbol,
                shares.getOrDefault(symbol, 0) + quantity
            );
        }

        boolean sell(String symbol, int quantity) {
            int owned = shares.getOrDefault(symbol, 0);

            if (quantity <= 0 || quantity > owned) {
                return false;
            }

            if (quantity == owned) {
                shares.remove(symbol);
            } else {
                shares.put(symbol, owned - quantity);
            }

            return true;
        }

        Map<String, Integer> getShares() {
            return Collections.unmodifiableMap(shares);
        }

        double getHoldingsValue(StockMarket market) {
            double total = 0;

            for (Map.Entry<String, Integer> e :
                    shares.entrySet()) {

                total += market.getStock(e.getKey()).getPrice()
                    * e.getValue();
            }

            return total;
        }

        double getTotalValue(StockMarket market) {
            return getHoldingsValue(market);
        }
    }

    static class User {

        private final String name;
        private double cash;

        private final Portfolio portfolio = new Portfolio();

        User(String name, double cash) {
            this.name = name;
            this.cash = cash;
        }

        String getName() {
            return name;
        }

        double getCash() {
            return cash;
        }

        void setCash(double cash) {
            this.cash = cash;
        }

        void addCash(double amount) {
            cash += amount;
        }

        Portfolio getPortfolio() {
            return portfolio;
        }

        void buy(String symbol, int quantity, double total) {
            cash -= total;
            portfolio.addShares(symbol, quantity);
        }
    }

    static class Transaction {

        private final String type;
        private final String symbol;
        private final String time;

        private final int quantity;
        private final double price;
        private final double total;

        Transaction(
                String type,
                String symbol,
                int quantity,
                double price,
                double total) {

            this.type = type;
            this.symbol = symbol;
            this.quantity = quantity;
            this.price = price;
            this.total = total;

            this.time = LocalTime.now().format(
                DateTimeFormatter.ofPattern("HH:mm:ss")
            );
        }

        String toStorageString() {
            return type + "|"
                + symbol + "|"
                + quantity + "|₹ "
                + DF.format(price) + "|₹ "
                + DF.format(total) + "|"
                + time;
        }
    }
static class ColorButton extends JButton {

    ColorButton(String text, Color color) {
        super(text);
        setBackground(color);
        setForeground(Color.WHITE);
        setOpaque(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();

        g2.setRenderingHint(
            RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON
        );

        g2.setColor(getBackground());
        g2.fillRoundRect(
            0, 0, getWidth(), getHeight(), 14, 14
        );

        g2.dispose();
        super.paintComponent(g);
    }
}

    static class RoundedPanel extends JPanel {

        private final Color color;
        private final int radius;

        RoundedPanel(Color color, int radius) {
            this.color = color;
            this.radius = radius;
            setOpaque(false);
        }

        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();

            g.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
            );

            g.setColor(color);

            g.fillRoundRect(
                0,
                0,
                getWidth() - 1,
                getHeight() - 1,
                radius,
                radius
            );

            g.setColor(new Color(100, 130, 180, 75));

            g.drawRoundRect(
                0,
                0,
                getWidth() - 1,
                getHeight() - 1,
                radius,
                radius
            );

            g.dispose();

            super.paintComponent(graphics);
        }
    }

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {

            try {
                UIManager.setLookAndFeel(
                    UIManager.getSystemLookAndFeelClassName()
                );
            } catch (Exception ignored) {
            }

            new Main().setVisible(true);
        });
    }
}