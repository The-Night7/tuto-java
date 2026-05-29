package io.github.perspectogame;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

final class UiFactory {
    static final Color BACKGROUND = new Color(0.06f, 0.07f, 0.14f, 1f);
    static final Color PANEL = new Color(0.08f, 0.10f, 0.20f, 0.88f);
    static final Color PANEL_LIGHT = new Color(0.12f, 0.16f, 0.30f, 0.94f);
    static final Color CYAN = new Color(0.12f, 0.93f, 1f, 1f);
    static final Color PINK = new Color(1f, 0.22f, 0.58f, 1f);
    static final Color YELLOW = new Color(1f, 0.82f, 0.18f, 1f);
    static final Color GREEN = new Color(0.30f, 1f, 0.45f, 1f);
    static final Color WHITE = new Color(0.96f, 0.98f, 1f, 1f);
    static final Color MUTED = new Color(0.64f, 0.72f, 0.88f, 1f);

    private UiFactory() {
    }

    static Skin createSkin() {
        Skin skin = new Skin();
        BitmapFont font = new BitmapFont();
        BitmapFont titleFont = new BitmapFont();
        BitmapFont smallFont = new BitmapFont();
        font.getData().setScale(1.05f);
        titleFont.getData().setScale(1.55f);
        smallFont.getData().setScale(0.88f);
        skin.add("default-font", font);
        skin.add("title-font", titleFont);
        skin.add("small-font", smallFont);

        addDrawable(skin, "panel", PANEL);
        addDrawable(skin, "panel-light", PANEL_LIGHT);
        addDrawable(skin, "button-up", new Color(0.13f, 0.17f, 0.34f, 0.96f));
        addDrawable(skin, "button-over", new Color(0.15f, 0.36f, 0.55f, 0.98f));
        addDrawable(skin, "button-down", new Color(0.32f, 0.13f, 0.46f, 1f));
        addDrawable(skin, "button-checked", new Color(0.58f, 0.16f, 0.44f, 1f));
        addDrawable(skin, "button-accent-up", new Color(0.08f, 0.48f, 0.62f, 1f));
        addDrawable(skin, "button-accent-over", new Color(0.10f, 0.70f, 0.82f, 1f));
        addDrawable(skin, "button-accent-down", new Color(0.78f, 0.20f, 0.52f, 1f));
        addDrawable(skin, "button-ghost", new Color(0.08f, 0.10f, 0.18f, 0.76f));
        addDrawable(skin, "hud", new Color(0.04f, 0.06f, 0.12f, 0.74f));
        addDrawable(skin, "progress-done", new Color(0.12f, 0.52f, 0.28f, 1f));
        addDrawable(skin, "progress-open", new Color(0.26f, 0.29f, 0.45f, 1f));

        Label.LabelStyle label = new Label.LabelStyle(font, WHITE);
        skin.add("default", label);
        skin.add("title", new Label.LabelStyle(titleFont, YELLOW));
        skin.add("small", new Label.LabelStyle(smallFont, MUTED));
        skin.add("accent", new Label.LabelStyle(font, CYAN));
        skin.add("success", new Label.LabelStyle(font, GREEN));
        skin.add("warning", new Label.LabelStyle(font, PINK));

        ScrollPane.ScrollPaneStyle scrollPane = new ScrollPane.ScrollPaneStyle();
        scrollPane.background = skin.getDrawable("panel");
        skin.add("default", scrollPane);

        TextButton.TextButtonStyle button = new TextButton.TextButtonStyle();
        button.font = font;
        button.fontColor = WHITE;
        button.overFontColor = YELLOW;
        button.downFontColor = WHITE;
        button.checkedFontColor = WHITE;
        button.up = skin.getDrawable("button-up");
        button.over = skin.getDrawable("button-over");
        button.down = skin.getDrawable("button-down");
        button.checked = skin.getDrawable("button-checked");
        skin.add("default", button);

        TextButton.TextButtonStyle accentButton = new TextButton.TextButtonStyle(button);
        accentButton.up = skin.getDrawable("button-accent-up");
        accentButton.over = skin.getDrawable("button-accent-over");
        accentButton.down = skin.getDrawable("button-accent-down");
        skin.add("accent", accentButton);

        TextButton.TextButtonStyle ghostButton = new TextButton.TextButtonStyle(button);
        ghostButton.up = skin.getDrawable("button-ghost");
        ghostButton.over = skin.getDrawable("button-over");
        ghostButton.down = skin.getDrawable("button-down");
        skin.add("ghost", ghostButton);

        return skin;
    }

    static Drawable drawable(Skin skin, String name) {
        return skin.getDrawable(name);
    }

    private static void addDrawable(Skin skin, String name, Color color) {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(color);
        pixmap.fill();
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        skin.add(name + "-texture", texture);
        skin.add(name, new TextureRegionDrawable(texture), Drawable.class);
    }
}
