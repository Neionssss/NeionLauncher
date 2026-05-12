package neion.nlchr2;

import android.graphics.drawable.Drawable;

public class AppItem {
    private final CharSequence label;
    private final String pkg;
    private final Drawable icon;

    public AppItem(CharSequence label, String pkg, Drawable icon) {
        this.label = label;
        this.pkg = pkg;
        this.icon = icon;
    }

    public CharSequence getLabel() {return label;}

    public String getPkg() {return pkg;}

    public Drawable getIcon() {return icon;}
}