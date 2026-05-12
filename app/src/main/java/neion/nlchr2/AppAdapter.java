package neion.nlchr2;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.List;
import java.util.Set;

public class AppAdapter extends BaseAdapter {

    public interface ClickListener {
        void onClick(String pkg);
    }

    public interface LongClickListener {
        void onLongClick(View view, String pkg);
    }

    private final Context context;
    private final ClickListener clickListener;
    private final LongClickListener longClickListener;
    private List<AppItem> items = List.of();
    private Set<String> hiddenPkgs = Set.of();
    private Set<String> favoritePkgs = Set.of();

    public AppAdapter(Context context, ClickListener clickListener, LongClickListener LongClickListener) {
        this.context = context;
        this.clickListener = clickListener;
        this.longClickListener = LongClickListener;
    }

    public void update(List<AppItem> newItems, Set<String> hidden, Set<String> favorites) {
        items = newItems;
        hiddenPkgs = hidden;
        favoritePkgs = favorites;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public AppItem getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view;
        Holder holder;

        if (convertView == null) {
            view = LayoutInflater.from(context).inflate(R.layout.item_app, parent, false);
            holder = new Holder(view);
            view.setTag(holder);
        } else {
            view = convertView;
            holder = (Holder) view.getTag();
        }

        AppItem app = getItem(position);
        String pkg = app.getPkg();

        holder.label.setText(app.getLabel());
        holder.label.setTextColor(favoritePkgs.contains(pkg) ? Color.YELLOW : Color.WHITE);
        holder.icon.setImageDrawable(app.getIcon());

        float alpha = hiddenPkgs.contains(pkg) ? 0.5f : 1f;
        holder.icon.setAlpha(alpha);
        holder.label.setAlpha(alpha);

        view.setOnClickListener(v -> clickListener.onClick(pkg));
        view.setOnLongClickListener(v -> {
            longClickListener.onLongClick(view, pkg);
            return true;
        });

        return view;
    }

    static class Holder {
        final TextView label;
        final ImageView icon;
        Holder(View view) {
            label = view.findViewById(R.id.label);
            icon = view.findViewById(R.id.icon);
        }
    }
}