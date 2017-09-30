package nostalgia.framework;

/**
 * Sound effect
 */
public abstract class SfxProfile {

    public String name;
    public boolean isStereo;
    public int rate;
    public int bufferSize;
    public SoundEncoding encoding;

    public int quality;

    public abstract int toInt();

    public enum SoundEncoding {
        PCM8, PCM16,
    }

}
