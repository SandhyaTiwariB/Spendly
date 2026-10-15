package com.spendly.service;

import com.spendly.entity.Category;
import com.spendly.entity.CategoryType;
import com.spendly.entity.TransactionDirection;
import com.spendly.entity.TransactionType;
import com.spendly.repository.CategoryRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class CategorizationService {

    private final CategoryRepository categoryRepository;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @org.springframework.beans.factory.annotation.Autowired
    public CategorizationService(CategoryRepository categoryRepository,
                                 @org.springframework.beans.factory.annotation.Autowired(required = false) org.springframework.jdbc.core.JdbcTemplate jdbcTemplate) {
        this.categoryRepository = categoryRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    public CategorizationService(CategoryRepository categoryRepository) {
        this(categoryRepository, null);
    }

    public static class CategoryResult {
        private final String category;
        private final TransactionType type;
        private final boolean isTransfer;

        public CategoryResult(String category, TransactionType type, boolean isTransfer) {
            this.category = category;
            this.type = type;
            this.isTransfer = isTransfer;
        }

        public String getCategory() { return category; }
        public TransactionType getType() { return type; }
        public boolean isTransfer() { return isTransfer; }
    }

    @PostConstruct
    public void initDefaultCategories() {
        if (jdbcTemplate != null) {
            try {
                jdbcTemplate.execute("ALTER TABLE categories ADD COLUMN IF NOT EXISTS is_custom BOOLEAN DEFAULT FALSE");
                jdbcTemplate.execute("ALTER TABLE categories ADD COLUMN IF NOT EXISTS user_id BIGINT");
                jdbcTemplate.execute("UPDATE categories SET is_custom = FALSE WHERE is_custom IS NULL");
            } catch (Exception ignored) {
            }
        }
        if (categoryRepository.count() == 0) {
            List<Category> defaults = List.of(
                    new Category("Income", CategoryType.INCOME, "#16865c", "↙"),
                    new Category("Food & Dining", CategoryType.EXPENSE, "#e67e22", "☕"),
                    new Category("Groceries", CategoryType.EXPENSE, "#27ae60", "🛒"),
                    new Category("Shopping", CategoryType.EXPENSE, "#9b59b6", "🛍"),
                    new Category("Transport", CategoryType.EXPENSE, "#3498db", "🚗"),
                    new Category("Entertainment", CategoryType.EXPENSE, "#e74c3c", "🎬"),
                    new Category("Health", CategoryType.EXPENSE, "#1abc9c", "💊"),
                    new Category("Education", CategoryType.EXPENSE, "#2980b9", "📚"),
                    new Category("Bills & Utilities", CategoryType.EXPENSE, "#f39c12", "⚡"),
                    new Category("Investments", CategoryType.EXPENSE, "#16a085", "📈"),
                    new Category("Housing", CategoryType.EXPENSE, "#8e44ad", "🏠"),
                    new Category("Personal Care", CategoryType.EXPENSE, "#d35400", "✂"),
                    new Category("Fees & Charges", CategoryType.EXPENSE, "#c0392b", "⚖"),
                    new Category("Transfer", CategoryType.TRANSFER, "#4f6ec9", "⇄"),
                    new Category("Other", CategoryType.EXPENSE, "#7f8c8d", "•")
            );
            categoryRepository.saveAll(defaults);
        }
    }

    public CategoryResult categorize(String description, TransactionDirection direction) {
        String desc = description != null ? description.toLowerCase() : "";

        // 1. Detect internal transfers first
        if (isInternalTransfer(desc)) {
            return new CategoryResult("Transfer", TransactionType.TRANSFER, true);
        }

        // 2. Credits / Income checks
        if (direction == TransactionDirection.CREDIT) {
            if (matches(desc, "salary", "payroll", "stipend", "wages", "dividend", "interest credited",
                    "bonus", "pension", "cash deposit", "client payment", "consulting fee", "freelance",
                    "remittance", "tax refund", "cashback", "credit interest", "int.pd", "int pd", "upicr", "reversal")) {
                return new CategoryResult("Income", TransactionType.INCOME, false);
            }
            if (matches(desc, "self", "own account", "own a/c", "transfer to self")) {
                return new CategoryResult("Transfer", TransactionType.TRANSFER, true);
            }
            return new CategoryResult("Income", TransactionType.INCOME, false);
        }

        // 3. Debits / Expense categorisation
        if (matches(desc, "swiggy", "zomato", "cafe", "coffee", "restaurant", "starbucks",
                "mcdonald", "burger", "pizza", "dining", "eatery", "baker", "barista", "dhaba",
                "tea", "chaat", "subway", "dominos", "kfc", "dine", "haldiram", "chai", "biryani",
                "food", "canteen", "bar", "pub", "brewery", "bikanervala", "sweet")) {
            return new CategoryResult("Food & Dining", TransactionType.EXPENSE, false);
        }

        if (matches(desc, "blinkit", "zepto", "instamart", "bigbasket", "supermarket",
                "grocery", "provisions", "dmart", "spencer", "nature basket", "kirana", "fruits", "vegetables",
                "reliance fresh", "reliance smart", "more retail", "super market", "dairy", "milk")) {
            return new CategoryResult("Groceries", TransactionType.EXPENSE, false);
        }

        if (matches(desc, "amazon", "flipkart", "myntra", "zara", "h&m", "retail", "clothing",
                "apparel", "store", "mall", "electronics", "croma", "reliancedigital", "ajio",
                "nykaa", "meesho", "decathlon", "lifestyle", "westside", "shopping", "uniqlo",
                "shoppers stop", "tata cliq", "apple store", "ikea", "footwear", "bata")) {
            return new CategoryResult("Shopping", TransactionType.EXPENSE, false);
        }

        if (matches(desc, "uber", "ola", "rapido", "metro", "fuel", "petrol", "diesel",
                "hpcl", "bpcl", "ioc", "iocl", "shell", "irctc", "railway", "flight", "indigo", "air india",
                "vistara", "spicejet", "akasa", "toll", "fastag", "taxi", "bus", "transport", "auto",
                "cab", "parking", "redbus", "makemytrip", "yatra", "cleartrip")) {
            return new CategoryResult("Transport", TransactionType.EXPENSE, false);
        }

        if (matches(desc, "electricity", "bescom", "tneb", "msedcl", "tata power", "adani electricity",
                "water", "broadband", "wifi", "airtel", "jio", "vi", "vodafone", "bsnl", "act fibernet",
                "gas", "piped gas", "indane", "bharat gas", "hp gas", "igl", "mgl", "utility",
                "billdesk", "bill desk", "recharge", "dth", "tata play", "sun direct", "dish tv", "bbps")) {
            return new CategoryResult("Bills & Utilities", TransactionType.EXPENSE, false);
        }

        if (matches(desc, "zerodha", "groww", "upstox", "angel one", "coin", "mutual fund", "sip",
                "kuvera", "smallcase", "indmoney", "stock", "shares", "ppf", "nps", "fixed deposit",
                "fd", "rd", "gold", "sebi", "demat", "bse", "nse", "investment")) {
            return new CategoryResult("Investments", TransactionType.EXPENSE, false);
        }

        if (matches(desc, "netflix", "spotify", "prime video", "hotstar", "disney", "cinema", "pvr",
                "inox", "cinepolis", "bookmyshow", "steam", "theatre", "movie", "youtube", "game",
                "gaming", "sony liv", "zee5", "apple music", "audible")) {
            return new CategoryResult("Entertainment", TransactionType.EXPENSE, false);
        }

        if (matches(desc, "pharmacy", "apollo", "1mg", "medplus", "hospital", "clinic", "netmeds",
                "doctor", "lab", "pathology", "diagnostic", "dental", "medicine", "health", "care", "optician",
                "lenskart", "practo", "max healthcare", "fortis")) {
            return new CategoryResult("Health", TransactionType.EXPENSE, false);
        }

        if (matches(desc, "rent", "maintenance", "society", "brokerage", "mortgage", "flat",
                "apartment", "house rent", "owner rent", "nobroker", "magicbricks")) {
            return new CategoryResult("Housing", TransactionType.EXPENSE, false);
        }

        if (matches(desc, "school", "college", "tuition", "course", "udemy", "coursera",
                "university", "books", "education", "fees school", "fees college", "edx", "coaching", "exam")) {
            return new CategoryResult("Education", TransactionType.EXPENSE, false);
        }

        if (matches(desc, "salon", "spa", "parlour", "gym", "fitness", "cult", "cult.fit", "barber",
                "cosmetics", "grooming", "urban company", "massage")) {
            return new CategoryResult("Personal Care", TransactionType.EXPENSE, false);
        }

        if (matches(desc, "annual fee", "late fee", "penalty", "gst", "sms alert",
                "maintenance charge", "interest charged", "bank charges", "atm fee", "convenience fee",
                "card fee", "overdue", "minimum balance", "consolidated charges")) {
            return new CategoryResult("Fees & Charges", TransactionType.EXPENSE, false);
        }

        // 4. Fallback for unclassified debit
        return new CategoryResult("Other", TransactionType.EXPENSE, false);
    }

    private boolean isInternalTransfer(String desc) {
        return matches(desc,
                "transfer to own",
                "transfer to self",
                "self transfer",
                "own a/c transfer",
                "own account transfer",
                "to savings a/c",
                "transfer to savings",
                "internal transfer",
                "sweep out",
                "sweep in",
                "inter-account transfer",
                "own account",
                "trf to own",
                "self trf"
        );
    }

    private boolean matches(String text, String... keywords) {
        for (String kw : keywords) {
            if (text.contains(kw)) {
                return true;
            }
        }
        return false;
    }
}
