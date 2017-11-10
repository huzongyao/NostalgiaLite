package nostalgia.framework;

/**
 * Graphic effect
 */
public abstract class GfxProfile {

    public String name;
    public int originalScreenWidth;
    public int originalScreenHeight;
    public int fps;

    public abstract int toInt();
}
