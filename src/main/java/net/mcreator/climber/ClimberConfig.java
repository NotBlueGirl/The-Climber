package net.mcreator.climber;

import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;

/**
 * Server-side runtime configuration for the Climber mod.
 * All fields are intentionally mutable – the /climb config commands write here directly.
 */
public class ClimberConfig {

    // -----------------------------------------------------------------------
    // Idle hand position defaults — MUST be declared before INSTANCE so that
    // the constructor body can safely reference them during static init.
    // -----------------------------------------------------------------------

    public static final Vec3 DEFAULT_IDLE_HAND_POS = new Vec3(0.0, 0.0, 0.0);

    // -----------------------------------------------------------------------
    // Singleton — declared AFTER the constants it depends on.
    // -----------------------------------------------------------------------

    public static final ClimberConfig INSTANCE = new ClimberConfig();

    private ClimberConfig() {}

    // -----------------------------------------------------------------------
    // Idle hand position (first-person "ready" pose offset from defaults)
    // -----------------------------------------------------------------------

    /** Additional translation applied to each idle (free) hand in first-person. */
    private Vec3 idleHandPos = DEFAULT_IDLE_HAND_POS;

    public Vec3 getIdleHandPos() {
        // Extra null-safety: if somehow null (shouldn't happen after fix), return zero
        return idleHandPos != null ? idleHandPos : DEFAULT_IDLE_HAND_POS;
    }
    public void  setIdleHandPos(Vec3 v)     { idleHandPos = (v != null) ? v : DEFAULT_IDLE_HAND_POS; }
    public void  resetIdleHandPos()         { idleHandPos = DEFAULT_IDLE_HAND_POS; }

    // -----------------------------------------------------------------------
    // Block exclude list  (blocks that CANNOT be grabbed even if solid)
    // -----------------------------------------------------------------------

    private final Set<String> excludeList = new HashSet<>();

    public Set<String> getExcludeList()           { return excludeList; }

    public void addExcludes(Iterable<String> ids)  { ids.forEach(excludeList::add); }
    public void resetExcludeList()                 { excludeList.clear(); }

    // -----------------------------------------------------------------------
    // Block include list  (when non-empty, ONLY these blocks can be grabbed)
    // -----------------------------------------------------------------------

    private final Set<String> includeList = new HashSet<>();

    public Set<String> getIncludeList()           { return includeList; }

    public void addIncludes(Iterable<String> ids)  { ids.forEach(includeList::add); }
    public void resetIncludeList()                 { includeList.clear(); }

    // -----------------------------------------------------------------------
    // Stamina system toggle
    // -----------------------------------------------------------------------

    private boolean staminaEnabled = false;

    public boolean isStaminaEnabled()            { return staminaEnabled; }
    public void    setStaminaEnabled(boolean v)  { staminaEnabled = v; }

    // -----------------------------------------------------------------------
    // Stamina constants
    // -----------------------------------------------------------------------

    public static final float STAMINA_MAX        = 100f;
    /** Drain per tick while a hand is pinned. */
    public static final float STAMINA_DRAIN_RATE = 0.66f;
    /** Recovery per tick while both hands are free. */
    public static final float STAMINA_REGEN_RATE = 0.375f;

    // -----------------------------------------------------------------------
    // Utility: check whether a block (by registry name) is grabbable
    // -----------------------------------------------------------------------

    /**
     * Returns true if the block identified by {@code registryName} is allowed
     * to be grabbed, given the current include / exclude lists.
     *
     * Rules:
     *  - If includeList is non-empty the block must be in it.
     *  - If the block is in excludeList it is forbidden regardless.
     */
    public boolean isGrabbable(String registryName) {
        if (!includeList.isEmpty() && !includeList.contains(registryName)) return false;
        if (excludeList.contains(registryName)) return false;
        return true;
    }
}
