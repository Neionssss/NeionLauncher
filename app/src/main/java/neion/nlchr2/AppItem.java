package neion.nlchr2;

import android.graphics.drawable.Drawable;

public class AppItem {
    public final CharSequence label;
    public final String pkg;
    public final Drawable icon;

    public AppItem(CharSequence label, String pkg, Drawable icon) {
        this.label = label;
        this.pkg = pkg;
        this.icon = icon;
    }
}