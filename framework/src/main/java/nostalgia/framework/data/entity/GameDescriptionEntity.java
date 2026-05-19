package nostalgia.framework.data.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "GameDescription",
        indices = {
                @Index(value = "checksum"),
                @Index(value = "inserTime"),
                @Index(value = "lastGameTime")
        })
public class GameDescriptionEntity {
    
    @PrimaryKey(autoGenerate = true)
    public long _id;
    
    public String name = "";
    
    public String path = "";
    
    public String checksum = "";
    
    public long zipfile_id = -1;
    
    public long inserTime = 0;
    
    public long lastGameTime = 0;
    
    public int runCount = 0;
}
