package com.wilfredchau.synapsepkb.category.model;

public enum Category {
    TECH("tech", "\u6280\u672f\u8d44\u6599"),
    NOVEL("novel", "\u5c0f\u8bf4"),
    COMIC("comic", "\u6f2b\u753b"),
    MOVIE("movie", "\u7535\u5f71"),
    NOTE("note", "\u751f\u6d3b\u7b14\u8bb0");

    private final String code;
    private final String displayName;

    Category(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public String code() { return code; }
    public String displayName() { return displayName; }
}
