package com.zeapo.pwdstore.ssh;

import java.io.File;

public class SshKeyItem {
    private String name;
    private File privateKey;
    private File publicKey;

    public SshKeyItem(String name) {
        this.name = name;
        privateKey = null;
        publicKey = null;
    }

    public void setPrivate(File privateKey) {
        this.privateKey = privateKey;
    }

    public void setPublic(File publicKey) {
        this.publicKey = publicKey;
    }

    public boolean hasPublic() { return publicKey != null; }

    public String getName() { return this.name; }

    public File getPublic() { return this.publicKey; }

    @Override
    public boolean equals(Object o){
        if (!(o instanceof SshKeyItem)) {
            return false;
        }
        SshKeyItem other = (SshKeyItem) o;
        return other.getName().equals(this.getName());
    }

}
