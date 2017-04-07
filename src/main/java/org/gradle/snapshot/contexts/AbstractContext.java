package org.gradle.snapshot.contexts;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.Maps;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import org.gradle.snapshot.files.Fileish;
import org.gradle.snapshot.files.PhysicalSnapshot;

import java.util.Collection;
import java.util.Map;

public abstract class AbstractContext implements Context {
    @VisibleForTesting
    final Map<String, Result> results = Maps.newLinkedHashMap();

    @Override
    public Class<? extends Context> getType() {
        return getClass();
    }

    @Override
    public void recordSnapshot(Fileish file, HashCode hash) {
        results.put(file.getPath(), new SnapshotResult(file, hash));
    }

    @Override
    public <C extends Context> C recordSubContext(Fileish file, Class<C> type) {
        String path = file.getPath();
        Result result = results.get(path);
        C subContext;
        if (result == null) {
            try {
                subContext = type.newInstance();
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
            final SubContextResult subContextResult = new SubContextResult(file, subContext);
            results.put(path, subContextResult);
        } else if (result instanceof SubContextResult) {
            Context resultSubContext = ((SubContextResult) result).getSubContext();
            subContext = type.cast(resultSubContext);
        } else {
            throw new IllegalStateException("Already has a non-context entry under path " + path);
        }
        return subContext;
    }

    @Override
    public final HashCode fold(ImmutableCollection.Builder<PhysicalSnapshot> physicalSnapshots) {
        return fold(results.entrySet(), physicalSnapshots);
    }

    protected HashCode fold(Collection<Map.Entry<String, Result>> results, ImmutableCollection.Builder<PhysicalSnapshot> physicalSnapshots) {
        Hasher hasher = Hashing.md5().newHasher();
        results.forEach(entry -> {
            String key = entry.getKey();
            Result result = entry.getValue();

            hasher.putString(key, Charsets.UTF_8);
            hasher.putBytes(result.fold(physicalSnapshots).asBytes());
        });
        return hasher.hash();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
}
