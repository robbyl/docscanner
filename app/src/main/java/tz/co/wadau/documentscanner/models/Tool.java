package tz.co.wadau.documentscanner.models;

/**
 * Created by Admin on 15-Jan-18.
 */

public class Tool {
    private int id;
    private String title;
    private String bgColor;
    private int drawable;

    public Tool(int id, String title, String pbColor, int drawable) {
        this.id = id;
        this.title = title;
        this.bgColor = pbColor;
        this.drawable = drawable;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBgColor() {
        return bgColor;
    }

    public void setBgColor(String bgColor) {
        this.bgColor = bgColor;
    }

    public int getDrawable() {
        return drawable;
    }

    public void setDrawable(int drawable) {
        this.drawable = drawable;
    }
}
