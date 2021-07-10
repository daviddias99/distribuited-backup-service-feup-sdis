package storage;

public class FileBackup extends BackupDB<FileInfo>{
    public FileBackup(String systemPath, String backupName) {
        super(systemPath, backupName);
    }

    public boolean addFile(FileInfo fileInfo) {
        return super.put(fileInfo.getID(), fileInfo);
    }
}