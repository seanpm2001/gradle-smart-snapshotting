package org.gradle.snapshot.files;

import com.google.common.hash.HashCode;

public interface PhysicalSnapshot {
    Physical getFile();
    HashCode getHashCode();
}
