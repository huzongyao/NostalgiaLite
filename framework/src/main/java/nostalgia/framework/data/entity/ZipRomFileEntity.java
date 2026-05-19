package nostalgia.framework.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "ZipRomFile")
public class ZipRomFileEntity {
    
    @PrimaryKey(autoGenerate = true)
    public long _id;
    
    public String hash = "";
    
    public String path = "";
}
