package com.tablatiempos.service;

import com.tablatiempos.domain.Entrant;
import com.tablatiempos.domain.PenaltyMode;
import com.tablatiempos.domain.Rally;
import com.tablatiempos.domain.ResultKind;
import com.tablatiempos.domain.Stage;
import com.tablatiempos.domain.StageResult;
import com.tablatiempos.domain.StageType;
import com.tablatiempos.dto.GeneralRowView;
import com.tablatiempos.dto.RallyView;
import com.tablatiempos.dto.RecordView;
import com.tablatiempos.dto.StageCellView;
import com.tablatiempos.dto.StageRowView;
import com.tablatiempos.dto.StageView;
import com.tablatiempos.repo.RallyRepository;
import com.tablatiempos.repo.StageResultRepository;
import com.tablatiempos.util.Names;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class RallyViewService {

    private static final String PENDING = "PENDING";
    private static final String LIVE = "LIVE";
    private static final String CLOSED = "CLOSED";
    private static final String VOID = "VOID";

    private static final String RANKED = "RANKED";
    private static final String EC = "EC";
    private static final String RETIRED = "RETIRED";

    private final RallyRepository rallyRepository;
    private final StageResultRepository resultRepository;
    private final RecordService recordService;

    public RallyViewService(RallyRepository rallyRepository,
                            StageResultRepository resultRepository,
                            RecordService recordService) {
        this.rallyRepository = rallyRepository;
        this.resultRepository = resultRepository;
        this.recordService = recordService;
    }

    @Transactional(readOnly = true)
    public RallyView view(Long rallyId) {
        Rally rally = rallyRepository.findById(rallyId)
                .orElseThrow(() -> new IllegalArgumentException("Rally no encontrado"));
        return build(rally);
    }

    @Transactional(readOnly = true)
    public RallyView viewByToken(String token) {
        Rally rally = rallyRepository.findByInviteToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Enlace no válido"));
        return build(rally);
    }

    private RallyView build(Rally rally) {
        Map<String, RecordView> records = recordService.byKey();

        List<Stage> stages = new ArrayList<>(rally.stages);
        stages.sort(Comparator.comparingInt(s -> s.position));
        List<Entrant> entrants = new ArrayList<>(rally.entrants);
        entrants.sort(Comparator.comparingInt(e -> e.position));

        Map<Long, Map<Long, StageResult>> resultsByStage = new HashMap<>();
        for (StageResult result : resultRepository.findByRallyId(rally.id)) {
            resultsByStage.computeIfAbsent(result.stageId, ignored -> new HashMap<>())
                    .put(result.entrantId, result);
        }

        Map<Long, StageCalc> calcs = new LinkedHashMap<>();
        Stage firstTc = stages.stream().filter(s -> s.type == StageType.TC).findFirst().orElse(null);

        // Primero los tramos TC
        for (Stage stage : stages) {
            if (stage.type == StageType.TC) {
                calcs.put(stage.id, calcTc(rally, stage, entrants,
                        resultsByStage.getOrDefault(stage.id, Map.of()), records));
            }
        }
        // El shakedown depende de si el primer TC ya tiene algún tiempo
        for (Stage stage : stages) {
            if (stage.type == StageType.SHAKEDOWN) {
                boolean firstTcStarted = firstTc != null
                        && !resultsByStage.getOrDefault(firstTc.id, Map.of()).isEmpty();
                calcs.put(stage.id, calcShakedown(stage, entrants,
                        resultsByStage.getOrDefault(stage.id, Map.of()), records, firstTcStarted));
            }
        }

        Stage current = stages.stream()
                .filter(s -> s.type == StageType.TC)
                .filter(s -> {
                    StageCalc c = calcs.get(s.id);
                    return PENDING.equals(c.status) || LIVE.equals(c.status);
                })
                .findFirst()
                .orElse(null);
        StageCalc currentCalc = current == null ? null : calcs.get(current.id);
        String currentStatus = currentCalc == null ? "FINISHED" : currentCalc.status;
        List<String> missingNames = currentCalc == null
                ? List.of()
                : currentCalc.missingIds.stream()
                .map(id -> entrantById(entrants, id).name)
                .toList();

        General general = calcGeneral(rally, stages, entrants, calcs, current, currentCalc,
                missingNames);

        List<StageView> stageViews = new ArrayList<>();
        for (Stage stage : stages) {
            StageCalc calc = calcs.get(stage.id);
            stageViews.add(buildStageView(rally, stage, entrants, calc, current, general));
        }

        return new RallyView(
                rally.id,
                rally.name,
                rally.date,
                rally.status.name(),
                rally.shakedownEnabled,
                rally.shakedownName,
                rally.penaltyMode.name(),
                rally.penaltyValueMs,
                rally.inviteToken,
                rally.version,
                stageViews,
                general.rows,
                entrants.stream()
                        .map(e -> new com.tablatiempos.dto.EntrantView(
                                e.id, e.name, e.car, e.position, e.retired, e.retiredAtPosition))
                        .toList(),
                current == null ? null : current.id,
                current == null ? "FINISHED" : current.code(),
                currentStatus,
                missingNames
        );
    }

    private StageCalc calcTc(Rally rally, Stage stage, List<Entrant> entrants,
                             Map<Long, StageResult> allResults, Map<String, RecordView> records) {
        StageCalc calc = new StageCalc();
        calc.stage = stage;
        calc.record = records.get(Names.key(stage.name));
        calc.recordMs = calc.record == null ? null : calc.record.recordMs();

        for (Entrant entrant : entrants) {
            if (retiredAt(entrant, stage)) {
                continue;
            }
            calc.activeIds.add(entrant.id);
            StageResult result = allResults.get(entrant.id);
            if (result == null) {
                calc.missingIds.add(entrant.id);
            } else {
                calc.results.put(entrant.id, result);
            }
        }

        Long bestValid = null;
        Long worstValid = null;
        for (StageResult result : calc.results.values()) {
            if (result.kind == ResultKind.VALID && result.millis != null) {
                bestValid = bestValid == null ? result.millis : Math.min(bestValid, result.millis);
                worstValid = worstValid == null ? result.millis : Math.max(worstValid, result.millis);
            }
        }
        calc.bestMs = bestValid;

        for (Map.Entry<Long, StageResult> entry : calc.results.entrySet()) {
            StageResult result = entry.getValue();
            Long millis = result.kind == ResultKind.VALID
                    ? result.millis
                    : penaltyMs(rally, worstValid);
            if (millis != null) {
                calc.ms.put(entry.getKey(), millis);
            }
        }

        boolean allHave = !calc.activeIds.isEmpty() && calc.activeIds.stream()
                .allMatch(calc.results::containsKey);
        long validCount = calc.results.values().stream()
                .filter(r -> r.kind == ResultKind.VALID && r.millis != null)
                .count();

        if (allHave && validCount == 0) {
            calc.status = VOID;
        } else if (allHave) {
            calc.status = CLOSED;
        } else if (!calc.results.isEmpty()) {
            calc.status = LIVE;
        } else {
            calc.status = PENDING;
        }

        calc.ranked = rank(calc, entrants);
        calc.bestEntrantId = calc.ranked.stream()
                .filter(r -> r.best)
                .map(r -> r.entrant.id)
                .findFirst()
                .orElse(null);
        return calc;
    }

    private StageCalc calcShakedown(Stage stage, List<Entrant> entrants,
                                    Map<Long, StageResult> allResults,
                                    Map<String, RecordView> records,
                                    boolean firstTcStarted) {
        StageCalc calc = new StageCalc();
        calc.stage = stage;
        calc.record = records.get(Names.key(stage.name));
        calc.recordMs = calc.record == null ? null : calc.record.recordMs();

        for (Entrant entrant : entrants) {
            calc.activeIds.add(entrant.id);
            StageResult result = allResults.get(entrant.id);
            if (result != null) {
                calc.results.put(entrant.id, result);
                if (result.millis != null) {
                    calc.ms.put(entrant.id, result.millis);
                }
            }
        }

        Long bestValid = null;
        for (Map.Entry<Long, Long> entry : calc.ms.entrySet()) {
            bestValid = bestValid == null ? entry.getValue() : Math.min(bestValid, entry.getValue());
        }
        calc.bestMs = bestValid;

        if (firstTcStarted) {
            calc.status = CLOSED;
        } else if (!calc.results.isEmpty()) {
            calc.status = LIVE;
        } else {
            calc.status = PENDING;
        }
        calc.ranked = rank(calc, entrants);
        calc.bestEntrantId = calc.ranked.stream()
                .filter(r -> r.best)
                .map(r -> r.entrant.id)
                .findFirst()
                .orElse(null);
        return calc;
    }

    private List<RankRow> rank(StageCalc calc, List<Entrant> entrants) {
        List<RankRow> rows = new ArrayList<>();
        for (Map.Entry<Long, Long> entry : calc.ms.entrySet()) {
            StageResult result = calc.results.get(entry.getKey());
            if (result == null) {
                continue;
            }
            RankRow row = new RankRow();
            row.entrant = entrantById(entrants, entry.getKey());
            row.ms = entry.getValue();
            row.penalty = result.kind == ResultKind.PENALTY;
            rows.add(row);
        }
        rows.sort(Comparator.comparingLong((RankRow r) -> r.ms)
                .thenComparingInt(r -> r.entrant.position));
        RankRow previous = null;
        boolean bestAssigned = false;
        for (int i = 0; i < rows.size(); i++) {
            RankRow row = rows.get(i);
            row.position = i + 1;
            row.bestGap = calc.bestMs == null ? null : row.ms - calc.bestMs;
            row.prevGap = previous == null ? null : row.ms - previous.ms;
            row.prevPosition = previous == null ? null : previous.position;
            row.prevName = previous == null ? null : previous.entrant.name;
            row.best = !row.penalty && !bestAssigned && Objects.equals(row.ms, calc.bestMs);
            if (row.best) {
                bestAssigned = true;
            }
            row.record = !row.penalty && calc.recordMs != null && Objects.equals(row.ms, calc.recordMs);
            previous = row;
        }
        return rows;
    }

    private Long penaltyMs(Rally rally, Long worstValid) {
        if (rally.penaltyMode == PenaltyMode.FIXED) {
            return rally.penaltyValueMs;
        }
        return worstValid == null ? null : worstValid + rally.penaltyValueMs;
    }

    private General calcGeneral(Rally rally, List<Stage> stages, List<Entrant> entrants,
                                Map<Long, StageCalc> calcs, Stage current,
                                StageCalc currentCalc, List<String> missingNames) {
        List<StageCalc> closed = stages.stream()
                .filter(s -> s.type == StageType.TC)
                .map(s -> calcs.get(s.id))
                .filter(c -> CLOSED.equals(c.status))
                .toList();

        Map<Long, Long> closedSum = new HashMap<>();
        for (Entrant entrant : entrants) {
            long sum = 0;
            for (StageCalc stage : closed) {
                Long millis = stage.ms.get(entrant.id);
                if (millis != null) {
                    sum += millis;
                }
            }
            closedSum.put(entrant.id, sum);
        }

        List<Entrant> active = entrants.stream().filter(e -> !e.retired).toList();
        List<GeneralRowView> ranked = new ArrayList<>();
        List<GeneralRowView> ec = new ArrayList<>();
        List<GeneralRowView> retired = new ArrayList<>();

        String currentStatus = currentCalc == null ? "FINISHED" : currentCalc.status;

        if (current == null) {
            for (Entrant entrant : active) {
                ranked.add(baseRanked(entrant, closedSum.get(entrant.id)));
            }
        } else if (PENDING.equals(currentStatus)) {
            if (closed.isEmpty()) {
                List<Entrant> ecOrder = ecOrder(entrants, active, calcs, current);
                for (Entrant entrant : ecOrder) {
                    ec.add(baseEc(entrant, closedSum.get(entrant.id)));
                }
            } else {
                for (Entrant entrant : active) {
                    ranked.add(baseRanked(entrant, closedSum.get(entrant.id)));
                }
            }
        } else if (LIVE.equals(currentStatus)) {
            for (Entrant entrant : active) {
                Long currentMs = currentCalc == null ? null : currentCalc.ms.get(entrant.id);
                if (currentMs != null) {
                    ranked.add(baseRanked(entrant, closedSum.get(entrant.id) + currentMs));
                } else {
                    ec.add(baseEc(entrant, closedSum.get(entrant.id)));
                }
            }
        }

        ranked.sort(Comparator.comparingLong((GeneralRowView r) -> r.totalMs())
                .thenComparingInt(r -> entrantById(entrants, r.entrantId()).position));
        GeneralRowView previous = null;
        for (int i = 0; i < ranked.size(); i++) {
            GeneralRowView row = ranked.get(i);
            int position = i + 1;
            Long difLeader = row.totalMs() - ranked.get(0).totalMs();
            Long difPrev = previous == null ? null : row.totalMs() - previous.totalMs();
            Integer prevPosition = previous == null ? null : previous.position();
            String prevName = previous == null ? null : previous.name();
            GeneralRowView enriched = new GeneralRowView(
                    position,
                    row.entrantId(),
                    row.name(),
                    row.car(),
                    RANKED,
                    row.totalMs(),
                    null,
                    difLeader,
                    difPrev,
                    prevPosition,
                    prevName,
                    i == 0
            );
            ranked.set(i, enriched);
            previous = enriched;
        }

        for (Entrant entrant : entrants) {
            if (entrant.retired) {
                retired.add(new GeneralRowView(
                        null, entrant.id, entrant.name, entrant.car, RETIRED,
                        null, null, null, null, null, null, false));
            }
        }

        List<GeneralRowView> rows = new ArrayList<>();
        rows.addAll(ranked);
        rows.addAll(ec);
        rows.addAll(retired);
        return new General(rows, ranked, ec, retired);
    }

    private List<Entrant> ecOrder(List<Entrant> entrants, List<Entrant> active,
                                  Map<Long, StageCalc> calcs, Stage current) {
        StageCalc sd = calcs.values().stream()
                .filter(c -> c.stage.type == StageType.SHAKEDOWN)
                .findFirst()
                .orElse(null);
        if (sd == null || sd.ms.isEmpty() || sd.status.equals(CLOSED)) {
            return active;
        }
        List<Entrant> sorted = new ArrayList<>(active);
        sorted.sort(Comparator.comparingLong((Entrant e) -> {
            Long ms = sd.ms.get(e.id);
            return ms == null ? Long.MAX_VALUE : ms;
        }).thenComparingInt(e -> e.position));
        return sorted;
    }

    private GeneralRowView baseRanked(Entrant entrant, Long total) {
        return new GeneralRowView(
                null, entrant.id, entrant.name, entrant.car, RANKED,
                total, null, null, null, null, null, false);
    }

    private GeneralRowView baseEc(Entrant entrant, Long partial) {
        return new GeneralRowView(
                null, entrant.id, entrant.name, entrant.car, EC,
                null, partial, null, null, null, null, false);
    }

    private StageView buildStageView(Rally rally, Stage stage, List<Entrant> entrants,
                                     StageCalc calc, Stage current, General general) {
        List<StageCellView> cells = new ArrayList<>();
        Map<Long, RankRow> rankById = new HashMap<>();
        for (RankRow row : calc.ranked) {
            rankById.put(row.entrant.id, row);
        }
        boolean provisional = LIVE.equals(calc.status) && stage.type == StageType.TC;

        for (Entrant entrant : entrants) {
            StageResult result = calc.results.get(entrant.id);
            RankRow rankRow = rankById.get(entrant.id);

            if (retiredAt(entrant, stage)) {
                cells.add(new StageCellView(entrant.id, null, RETIRED,
                        false, false, null, null, null, null, false));
                continue;
            }
            if (result != null) {
                Long millis = calc.ms.get(entrant.id);
                boolean penalty = result.kind == ResultKind.PENALTY;
                boolean best = rankRow != null && rankRow.best;
                boolean record = rankRow != null && rankRow.record;
                cells.add(new StageCellView(
                        entrant.id,
                        millis,
                        penalty ? "PENALTY" : "VALID",
                        best,
                        record,
                        rankRow == null ? null : rankRow.bestGap,
                        rankRow == null ? null : rankRow.prevGap,
                        rankRow == null ? null : rankRow.prevPosition,
                        rankRow == null ? null : rankRow.prevName,
                        provisional
                ));
                continue;
            }
            if (current != null && Objects.equals(current.id, stage.id)) {
                cells.add(new StageCellView(entrant.id, null, "EC",
                        false, false, null, null, null, null, false));
                continue;
            }
            cells.add(new StageCellView(entrant.id, null, "NONE",
                    false, false, null, null, null, null, false));
        }

        List<StageRowView> rows = new ArrayList<>();
        for (RankRow row : calc.ranked) {
            rows.add(new StageRowView(
                    row.position,
                    row.entrant.id,
                    row.entrant.name,
                    row.entrant.car,
                    row.ms,
                    row.penalty ? "PENALTY" : "VALID",
                    row.best,
                    row.record,
                    row.bestGap,
                    row.prevGap,
                    row.prevPosition,
                    row.prevName
            ));
        }

        List<String> missing = calc.missingIds.stream()
                .map(id -> entrantById(entrants, id).name)
                .toList();

        return new StageView(
                stage.id,
                stage.code(),
                stage.name,
                stage.type.name(),
                stage.position,
                calc.status,
                calc.bestMs,
                calc.bestEntrantId,
                calc.recordMs,
                calc.record == null ? null : calc.record.holder(),
                calc.record == null ? null : calc.record.car(),
                calc.record == null ? null : calc.record.rally(),
                calc.record == null || calc.record.date() == null ? null : calc.record.date().toString(),
                cells,
                rows,
                missing
        );
    }

    private boolean retiredAt(Entrant entrant, Stage stage) {
        if (!entrant.retired) {
            return false;
        }
        Integer at = entrant.retiredAtPosition;
        return at == null || stage.position >= at;
    }

    private Entrant entrantById(List<Entrant> entrants, Long id) {
        return entrants.stream().filter(e -> Objects.equals(e.id, id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Piloto no encontrado"));
    }

    private static final class StageCalc {
        Stage stage;
        String status;
        Set<Long> activeIds = new HashSet<>();
        Set<Long> missingIds = new HashSet<>();
        Map<Long, StageResult> results = new HashMap<>();
        Map<Long, Long> ms = new HashMap<>();
        Long bestMs;
        Long bestEntrantId;
        Long recordMs;
        RecordView record;
        List<RankRow> ranked = new ArrayList<>();
    }

    private static final class RankRow {
        Entrant entrant;
        long ms;
        boolean penalty;
        int position;
        Long bestGap;
        Long prevGap;
        Integer prevPosition;
        String prevName;
        boolean best;
        boolean record;
    }

    private record General(
            List<GeneralRowView> rows,
            List<GeneralRowView> ranked,
            List<GeneralRowView> ec,
            List<GeneralRowView> retired
    ) {
    }
}
