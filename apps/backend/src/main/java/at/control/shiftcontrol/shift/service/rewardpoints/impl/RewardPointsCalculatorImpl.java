package at.control.shiftcontrol.shift.service.rewardpoints.impl;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;

import at.shiftcontrol.lib.entity.PositionSlot;
import at.shiftcontrol.lib.entity.Role;
import at.shiftcontrol.lib.entity.ShiftPlan;
import at.shiftcontrol.lib.exception.IllegalArgumentException;
import at.shiftcontrol.lib.util.TimeUtil;
import at.shiftcontrol.shiftservice.dto.rewardpoints.RewardPointsSnapshotDto;
import at.shiftcontrol.shiftservice.service.rewardpoints.RewardPointsCalculator;

@Service
@RequiredArgsConstructor
public class RewardPointsCalculatorImpl implements RewardPointsCalculator {
    @Override
    public RewardPointsSnapshotDto calculateForAssignment(@NonNull PositionSlot slot) {
        var shift = slot.getShift();

        int durationMinutes = Math.toIntExact(TimeUtil.calculateDurationInMinutesCeil(shift.getStartTime(), shift.getEndTime()));
        int shiftBonus = shift.getBonusRewardPoints();

        Integer fixedOverride = slot.getOverrideRewardPoints(); // nullable
        Integer ppm = null;

        int basePoints;
        if (fixedOverride != null) {
            basePoints = fixedOverride;
        } else {
            ppm = resolvePointsPerMinute(slot);
            basePoints = multiplyPoints(durationMinutes, ppm);
        }

        int accepted = safeAdd(basePoints, shiftBonus);

        Map<String, Object> meta = new HashMap<>();
        meta.put("durationMinutes", durationMinutes);
        meta.put("slotFixedOverrideUsed", fixedOverride != null);
        if (fixedOverride != null) {
            meta.put("slotFixedPoints", fixedOverride);
        } else {
            meta.put("pointsPerMinute", ppm);
        }
        meta.put("basePoints", basePoints);
        meta.put("shiftBonusPoints", shiftBonus);
        meta.put("acceptedRewardPoints", accepted);

        meta.put("positionSlotId", slot.getId());
        meta.put("shiftId", shift.getId());

        return new RewardPointsSnapshotDto(accepted, meta);
    }

    private int resolvePointsPerMinute(PositionSlot slot) {
        Role role = slot.getRole(); // nullable
        if (role != null) {
            return role.getRewardPointsPerMinute();
        }

        ShiftPlan shiftPlan = slot.getShift().getShiftPlan();

        return shiftPlan.getDefaultNoRolePointsPerMinute();
    }

    private int multiplyPoints(int minutes, int ppm) {
        if (minutes < 0) {
            throw new IllegalArgumentException("minutes must be >= 0");
        }
        if (ppm < 0) {
            throw new IllegalArgumentException("ppm must be >= 0");
        }
        long res = (long) minutes * (long) ppm;
        if (res > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("points overflow: " + res);
        }
        return (int) res;
    }

    private int safeAdd(int a, int b) {
        long res = (long) a + (long) b;
        if (res > Integer.MAX_VALUE || res < Integer.MIN_VALUE) {
            throw new IllegalArgumentException("points overflow: " + res);
        }
        return (int) res;
    }

    @Override
    public String calculatePointsConfigHash(PositionSlot slot) {
        var shift = slot.getShift();

        String data = String.join("|",
            "slot=" + slot.getId(),
            "shift=" + shift.getId(),
            "override=" + slot.getOverrideRewardPoints(),
            "role=" + (slot.getRole() != null ? slot.getRole().getId() : "none"),
            "rolePpm=" + (slot.getRole() != null ? slot.getRole().getRewardPointsPerMinute() : "none"),
            "shiftBonus=" + shift.getBonusRewardPoints(),
            "start=" + shift.getStartTime().toEpochMilli(),
            "end=" + shift.getEndTime().toEpochMilli(),
            "defaultNoRolePpm=" + shift.getShiftPlan().getDefaultNoRolePointsPerMinute()
        );

        return DigestUtils.sha256Hex(data);
    }
}
