// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.audio;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.iso.IsoUtils;
import zombie.popman.ObjectPool;
import zombie.scripting.objects.CharacterTrait;

public final class SoundInstanceLimiter {
    private static final ObjectPool<SoundInstanceLimiter.SoundInstance> pool = new ObjectPool<>(SoundInstanceLimiter.SoundInstance::new);
    private static final SoundInstanceLimiter.DistanceComparator distanceComparator = new SoundInstanceLimiter.DistanceComparator();
    private final Map<String, SoundInstanceLimiter.SoundInstanceGroup> groups = new HashMap<>();

    public void startFrame() {
        for (Entry<String, SoundInstanceLimiter.SoundInstanceGroup> it : this.groups.entrySet()) {
            it.getValue().startFrame();
        }
    }

    public void getActiveGroups(List<String> activeGroups) {
        activeGroups.clear();

        for (Entry<String, SoundInstanceLimiter.SoundInstanceGroup> it : this.groups.entrySet()) {
            if (!it.getValue().instances.isEmpty()) {
                it.getValue().sortByDistance(distanceComparator);
                activeGroups.add(it.getKey());
            }
        }
    }

    public void register(Object object, String soundName, float x, float y, float z) {
        SoundInstanceLimiter.SoundInstanceGroup group = this.groups.get(soundName);
        if (group == null) {
            group = new SoundInstanceLimiter.SoundInstanceGroup(soundName);
            this.groups.put(soundName, group);
        }

        group.register(object, x, y, z);
    }

    public boolean isClosest(Object object, String soundName) {
        SoundInstanceLimiter.SoundInstanceGroup group = this.groups.get(soundName);
        return group != null ? group.isClosest(object) : false;
    }

    private static final class DistanceComparator implements Comparator<SoundInstanceLimiter.SoundInstance> {
        public int compare(SoundInstanceLimiter.SoundInstance o1, SoundInstanceLimiter.SoundInstance o2) {
            float aScore = this.getClosestListener(o1.x, o1.y, o1.z);
            float bScore = this.getClosestListener(o2.x, o2.y, o2.z);
            return Float.compare(aScore, bScore);
        }

        float getClosestListener(float soundX, float soundY, float soundZ) {
            IsoGameCharacter listener = null;
            float minDist = Float.MAX_VALUE;

            for (int i = 0; i < IsoPlayer.numPlayers; i++) {
                IsoGameCharacter chr = IsoPlayer.players[i];
                if (chr != null && (!chr.hasTrait(CharacterTrait.DEAF) || listener == null || listener.hasTrait(CharacterTrait.DEAF))) {
                    float px = chr.getX();
                    float py = chr.getY();
                    float pz = chr.getZ();
                    float dist = IsoUtils.DistanceTo(px, py, pz * 3.0F, soundX, soundY, soundZ * 3.0F);
                    dist *= chr.getHearDistanceModifier();
                    if (dist < minDist) {
                        minDist = dist;
                        listener = chr;
                    }
                }
            }

            return minDist;
        }
    }

    private static final class SoundInstance {
        Object object;
        float x;
        float y;
        float z;

        SoundInstanceLimiter.SoundInstance set(Object object, float x, float y, float z) {
            this.object = object;
            this.x = x;
            this.y = y;
            this.z = z;
            return this;
        }
    }

    private static final class SoundInstanceGroup {
        String soundName;
        final List<SoundInstanceLimiter.SoundInstance> instances = new ArrayList<>();

        SoundInstanceGroup(String soundName) {
            this.soundName = soundName;
        }

        void startFrame() {
            SoundInstanceLimiter.pool.releaseAll(this.instances);
            this.instances.clear();
        }

        SoundInstanceLimiter.SoundInstance findObject(Object object) {
            for (SoundInstanceLimiter.SoundInstance instance : this.instances) {
                if (instance.object == object) {
                    return instance;
                }
            }

            return null;
        }

        void register(Object object, float x, float y, float z) {
            SoundInstanceLimiter.SoundInstance instance = this.findObject(object);
            if (instance == null) {
                this.instances.add(SoundInstanceLimiter.pool.alloc().set(object, x, y, z));
            } else {
                instance.x = x;
                instance.y = y;
                instance.z = z;
            }
        }

        void sortByDistance(SoundInstanceLimiter.DistanceComparator distanceComparator) {
            this.instances.sort(distanceComparator);
        }

        boolean isClosest(Object object) {
            return !this.instances.isEmpty() && object == this.instances.getFirst().object;
        }
    }
}
