package storage;

public class BackupSystemException extends Exception {
    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public BackupSystemException(String message) { super(message); }
    public BackupSystemException(String message, Throwable cause) { super(message, cause); }
    public BackupSystemException(Throwable cause) { super(cause); }
  }