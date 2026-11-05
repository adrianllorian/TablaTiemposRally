package com.tablatiempos.service;

import com.tablatiempos.domain.Entrant;
import com.tablatiempos.domain.ResultKind;
import com.tablatiempos.domain.Stage;
import com.tablatiempos.domain.StageResult;
import com.tablatiempos.dto.RallyRef;
import com.tablatiempos.dto.RecordEntryView;
import com.tablatiempos.dto.RecordView;
import com.tablatiempos.repo.EntrantRepository;
import com.tablatiempos.repo.RallyRepository;
import com.tablatiempos.repo.StageRepository;
import com.tablatiempos.repo.StageResultRepository;
import com.tablatiempos.util.Names;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class RecordService {

    private final StageResultRepository resultRepository;
    private final StageRepository stageRepository;
    private final EntrantRepository entrantRepository;
    private final RallyRepository rallyRepository;

    public RecordService(StageResultRepository resultRepository,
                         StageRepository stageRepository,
                         EntrantRepository entrantRepository,
                         RallyRepository rallyRepository) {
        this.resultRepository = resultRepository;
        this.stageRepository = stageRepository;
        this.entrantRepository = entrantRepository;
        this.rallyRepository = rallyRepository;
    }

    private record Item(
            String key,
            String name,
            String type,
            long millis,
            String pilot,
            String car,
            String rally,
            java.time.LocalDate date
    ) {
    }

    @Transactional(readOnly = true)
    public List<RecordView> all() {
        Map<Long, Stage> stages = new HashMap<>();
        for (Stage stage : stageRepository.findAll()) {
            stages.put(stage.id, stage);
        }
        Map<Long, Entrant> entrants = new HashMap<>();
        for (Entrant entrant : entrantRepository.findAll()) {
            entrants.put(entrant.id, entrant);
        }
        Map<Long, RallyRef> rallies = new HashMap<>();
        for (RallyRef rally : rallyRepository.findAllRefs()) {
            rallies.put(rally.id(), rally);
        }

        Map<String, List<Item>> grouped = new LinkedHashMap<>();
        for (StageResult result : resultRepository.findAll()) {
            if (result.kind != ResultKind.VALID || result.millis == null) {
                continue;
            }
            Stage stage = stages.get(result.stageId);
            Entrant entrant = entrants.get(result.entrantId);
            RallyRef rally = rallies.get(result.rallyId);
            if (rally == null && rallies.size() == 1) {
                rally = rallies.values().iterator().next();
            }
            if (stage == null || entrant == null || rally == null) {
                continue;
            }
            String key = Names.key(stage.name);
            if (key.isBlank()) {
                continue;
            }
            Item item = new Item(
                    key,
                    Names.clean(stage.name),
                    stage.type.name(),
                    result.millis,
                    entrant.name,
                    entrant.car,
                    rally.name(),
                    rally.date()
            );
            grouped.computeIfAbsent(key, ignored -> new ArrayList<>()).add(item);
        }

        List<RecordView> records = new ArrayList<>();
        for (List<Item> items : grouped.values()) {
            items.sort(Comparator.comparingLong(Item::millis)
                    .thenComparing(Item::date, Comparator.nullsLast(Comparator.naturalOrder())));
            Item best = items.get(0);
            List<RecordEntryView> top = new ArrayList<>();
            for (int i = 0; i < Math.min(5, items.size()); i++) {
                Item item = items.get(i);
                top.add(new RecordEntryView(
                        i + 1,
                        item.pilot,
                        item.car,
                        item.millis,
                        item.rally,
                        item.date
                ));
            }
            records.add(new RecordView(
                    best.name,
                    best.key,
                    best.type,
                    best.millis,
                    best.pilot,
                    best.car,
                    best.rally,
                    best.date,
                    top
            ));
        }
        records.sort(Comparator.comparing(RecordView::name, String.CASE_INSENSITIVE_ORDER));
        return records;
    }

    @Transactional(readOnly = true)
    public Map<String, RecordView> byKey() {
        Map<String, RecordView> map = new LinkedHashMap<>();
        for (RecordView record : all()) {
            map.put(record.key(), record);
        }
        return map;
    }
}
