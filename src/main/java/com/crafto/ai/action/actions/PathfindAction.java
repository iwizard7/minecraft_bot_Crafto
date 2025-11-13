package com.crafto.ai.action.actions;

import com.crafto.ai.action.ActionResult;
import com.crafto.ai.action.Task;
import com.crafto.ai.entity.CraftoEntity;
import net.minecraft.core.BlockPos;

public class PathfindAction extends BaseAction {
    private BlockPos targetPos;
    private int ticksRunning;
    private static final int MAX_TICKS = 600; // 30 seconds timeout

    public PathfindAction(CraftoEntity crafto, Task task) {
        super(crafto, task);
    }

    @Override
    protected void onStart() {
        int x = task.getIntParameter("x", 0);
        int y = task.getIntParameter("y", 0);
        int z = task.getIntParameter("z", 0);
        
        targetPos = new BlockPos(x, y, z);
        ticksRunning = 0;
        
        crafto.getNavigation().moveTo(x, y, z, 1.0);
    }

    @Override
    protected void onTick() {
        ticksRunning++;
        
        if (crafto.blockPosition().closerThan(targetPos, 2.0)) {
            result = ActionResult.success("Reached target position");
            return;
        }
        
        if (ticksRunning > MAX_TICKS) {
            result = ActionResult.failure("Pathfinding timeout");
            return;
        }
        
        if (crafto.getNavigation().isDone() && !crafto.blockPosition().closerThan(targetPos, 2.0)) {
            crafto.getNavigation().moveTo(targetPos.getX(), targetPos.getY(), targetPos.getZ(), 1.0);
        }
    }

    @Override
    protected void onCancel() {
        crafto.getNavigation().stop();
    }

    @Override
    public String getDescription() {
        return "Pathfind to " + targetPos;
    }
}

