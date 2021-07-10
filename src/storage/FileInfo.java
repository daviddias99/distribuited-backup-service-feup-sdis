package storage;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileOwnerAttributeView;
import java.nio.file.attribute.UserPrincipal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;

import javax.naming.directory.InvalidAttributeValueException;

import utils.Protocol;

public class FileInfo implements Serializable {

    private static final long serialVersionUID = 46987258900228L;
    private File file;
    private String id;
    private int nChunks;
    private int replicationDegree;
    private ArrayList<byte[]> chunks;

    public FileInfo(String filepath, int replicationDegree) throws InvalidAttributeValueException {
        if (filepath == null)
            throw new InvalidAttributeValueException("Filepath can't be null");
        if (replicationDegree < 1)
            throw new InvalidAttributeValueException("Replication Degree must be at least 1");

        this.file = new File(filepath);

        if (!this.file.exists()) {
            throw new InvalidAttributeValueException("File does not exist");
        }

        Path path = Paths.get(filepath);
        FileOwnerAttributeView ownerAttributeView = Files.getFileAttributeView(path, FileOwnerAttributeView.class);
        UserPrincipal owner = null;
        String ownerName = "default";

        try {
            owner = ownerAttributeView.getOwner();
            ownerName = owner.getName();
        } catch (IOException e1) {

            e1.printStackTrace();
        }

        this.replicationDegree = replicationDegree;

        String metaFileName = this.file.getName() + String.valueOf(this.file.lastModified()) + ownerName;
        try {
            this.id = this.sha256(metaFileName);
        } catch (NoSuchAlgorithmException e) {
            System.out.println("FileInfo error initializing the id");
            e.printStackTrace();
        }

        this.chunks = new ArrayList<>();
        this.nChunks = calcNChunks();
    }

    public FileInfo(String filepath) throws InvalidAttributeValueException {
        if (filepath == null)
            throw new InvalidAttributeValueException("Filepath can't be null");

        Path path = Paths.get(filepath);
        FileOwnerAttributeView ownerAttributeView = Files.getFileAttributeView(path, FileOwnerAttributeView.class);
        UserPrincipal owner = null;
        String ownerName = "default";

        try {
            owner = ownerAttributeView.getOwner();
            ownerName = owner.getName();
        } catch (IOException e1) {

            e1.printStackTrace();
        }

        this.file = new File(filepath);
        String metaFileName = this.file.getName() + String.valueOf(this.file.lastModified()) + ownerName;
        try {
            this.id = this.sha256(metaFileName);
        } catch (NoSuchAlgorithmException e) {
            System.out.println("FileInfo error initializing the id");
            e.printStackTrace();
        }
        this.nChunks = calcNChunks();
    }

    public String getFilepath() {
        return this.file.getPath();
    }

    private int calcNChunks() throws InvalidAttributeValueException {
        if (this.file.length() > (long) Protocol.MAX_CHUNKS * (long) Protocol.MAX_CHUNK_SIZE)
            throw new InvalidAttributeValueException("File is too big");

        return (int) (Math.floor(this.file.length() / Protocol.MAX_CHUNK_SIZE) + 1);
    }

    public ArrayList<byte[]> getChunks() throws IOException {
        byte[] fileData = Files.readAllBytes(file.toPath());
        for (int i = 0; i < this.nChunks; i++) {
            byte[] chunk;
            if (i == this.nChunks - 1 && fileData.length % Protocol.MAX_CHUNK_SIZE == 0)
                chunk = new byte[0];
            else if (i == this.nChunks - 1)
                chunk = Arrays.copyOfRange(fileData, i * Protocol.MAX_CHUNK_SIZE, fileData.length);
            else
                chunk = Arrays.copyOfRange(fileData, i * Protocol.MAX_CHUNK_SIZE, (i + 1) * Protocol.MAX_CHUNK_SIZE);

            this.chunks.add(chunk);
        }

        return this.chunks;
    }

    public String getID() {
        return this.id;
    }

    public int getRepDegree() {
        return this.replicationDegree;
    }

    public int getNumberChunks() {
        return this.nChunks;
    }

    public String getFileName() {

        return this.file.getName();

    }

    private final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    public String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    public String sha256(String str) throws NoSuchAlgorithmException {
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        byte[] hash = sha.digest(str.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(hash);
    }
}