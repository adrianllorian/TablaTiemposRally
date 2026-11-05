package com.tablatiempos.service;

import com.tablatiempos.domain.Entrant;
import com.tablatiempos.domain.PenaltyMode;
import com.tablatiempos.domain.Rally;
import com.tablatiempos.domain.RallyStatus;
import com.tablatiempos.domain.ResultKind;
import com.tablatiempos.domain.Stage;
import com.tablatiempos.domain.StageResult;
import com.tablatiempos.domain.StageType;
import com.tablatiempos.dto.GeneralRowView;
import com.tablatiempos.dto.RallyConfigRequest;
import com.tablatiempos.dto.RallySummary;
import com.tablatiempos.dto.RallyView;
import com.tablatiempos.dto.ResultRequest;
import com.tablatiempos.dto.StageView;
import com.tablatiempos.repo.RallyRepository;
import com.tablatiempos.repo.StageResultRepository;
import com.tablatiempos.util.Names;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class RallyService {

    private final RallyRepository rallyRepository;
    private final StageResultRepository resultRepository;
    private final RallyViewService viewService;

    public RallyService(RallyRepository rallyRepository,
                        StageResultRepository resultRepository,
                        RallyViewService viewService) {
        this.rallyRepository = rallyRepository;
        this.resultRepository = resultRepository;
        this.viewService = viewService;
    }

    @Transactional(readOnly = true)
    public List<RallySummary> list() {
        List<RallySummary> result = new ArrayList<>();
        for (Rally rally : rallyRepository.findAllByOrderByCreatedAtDesc()) {
            RallyView view = viewService.view(rally.id);
            String leader = view.general().stream()
                    .filter(r -> "RANKED".equals(r.status()))
                    .findFirst()
                    .map(GeneralRowView::name)
                    .orElse("—");
            List<StageView> tcs = view.stages().stream()
                    .filter(s -> "TC".equals(s.type()))
                    .toList();
            int closed = (int) tcs.stream()
                    .filter(s -> "CLOSED".equals(s.status()) || "VOID".equals(s.status()))
                    .count();
            boolean active = rally.status == RallyStatus.ACTIVE;
            result.add(new RallySummary(
                    rally.id,
                    rally.name,
                    rally.date,
                    rally.status.name(),
                    leader,
                    active ? null : leader,
                    tcs.size(),
                    closed,
                    active,
                    rally.inviteToken
            ));
        }
        return result;
    }

    @Transactional
    public RallyView create(RallyConfigRequest request) {
        if (request == null || request.stages() == null || request.stages().isEmpty()) {
            throw badRequest("El rally necesita al menos un tramo");
        }
        if (request.entrants() == null || request.entrants().isEmpty()) {
            throw badRequest("El rally necesita al menos un piloto");
        }

        rallyRepository.findFirstByStatusOrderByCreatedAtDesc(RallyStatus.ACTIVE)
                .ifPresent(active -> {
                    active.status = RallyStatus.FINISHED;
                    active.updatedAt = Instant.now();
                    active.version++;
                    rallyRepository.save(active);
                });

        Rally rally = new Rally();
        rally.name = blank(request.name()) ? "Rally " + LocalDate.now() : Names.clean(request.name());
        rally.date = LocalDate.now();
        rally.status = RallyStatus.ACTIVE;
        rally.penaltyMode = penaltyMode(request.penaltyMode());
        rally.penaltyValueMs = penaltyValue(request.penaltyValueMs());
        rally.shakedownEnabled = Boolean.TRUE.equals(request.shakedownEnabled());
        rally.shakedownName = Names.clean(request.shakedownName());
        rally.inviteToken = newInviteToken();
        rally.createdAt = Instant.now();
        rally.updatedAt = rally.createdAt;
        rally.version = 0;

        if (rally.shakedownEnabled) {
            Stage sd = new Stage();
            sd.rally = rally;
            sd.position = 0;
            sd.type = StageType.SHAKEDOWN;
            sd.name = blank(rally.shakedownName) ? "Shakedown" : rally.shakedownName;
            rally.stages.add(sd);
        }

        int position = 1;
        for (RallyConfigRequest.StageInput input : request.stages()) {
            Stage stage = new Stage();
            stage.rally = rally;
            stage.position = position++;
            stage.type = StageType.TC;
            stage.name = input == null ? "" : Names.clean(input.name());
            rally.stages.add(stage);
        }

        int order = 0;
        for (RallyConfigRequest.EntrantInput input : request.entrants()) {
            if (input == null || blank(input.name())) {
                throw badRequest("Todos los pilotos necesitan nombre");
            }
            Entrant entrant = new Entrant();
            entrant.rally = rally;
            entrant.position = order++;
            entrant.name = Names.clean(input.name());
            entrant.car = Names.clean(input.car());
            rally.entrants.add(entrant);
        }

        rallyRepository.save(rally);
        return viewService.view(rally.id);
    }

    @Transactional
    public RallyView updateConfig(Long rallyId, RallyConfigRequest request) {
        Rally rally = getRally(rallyId);
        if (request == null) {
            throw badRequest("Configuración vacía");
        }
        if (request.stages() == null || request.stages().isEmpty()) {
            throw badRequest("El rally necesita al menos un tramo");
        }
        if (request.entrants() == null || request.entrants().isEmpty()) {
            throw badRequest("El rally necesita al menos un piloto");
        }

        rally.name = blank(request.name()) ? rally.name : Names.clean(request.name());
        rally.penaltyMode = penaltyMode(request.penaltyMode());
        rally.penaltyValueMs = penaltyValue(request.penaltyValueMs());

        boolean shakedownEnabled = Boolean.TRUE.equals(request.shakedownEnabled());
        Stage existingSd = rally.shakedown();
        if (shakedownEnabled) {
            if (existingSd == null) {
                Stage sd = new Stage();
                sd.rally = rally;
                sd.position = 0;
                sd.type = StageType.SHAKEDOWN;
                sd.name = blank(request.shakedownName()) ? "Shakedown" : Names.clean(request.shakedownName());
                rally.stages.add(sd);
                existingSd = sd;
            }
            existingSd.name = blank(request.shakedownName()) ? "Shakedown" : Names.clean(request.shakedownName());
        } else if (existingSd != null) {
            removeStage(rally, existingSd);
        }
        rally.shakedownEnabled = shakedownEnabled;
        rally.shakedownName = Names.clean(request.shakedownName());

        List<Stage> currentTc = rally.tcStages();
        List<RallyConfigRequest.StageInput> desiredStages = request.stages();
        for (int i = 0; i < desiredStages.size(); i++) {
            String name = desiredStages.get(i) == null ? "" : Names.clean(desiredStages.get(i).name());
            if (i < currentTc.size()) {
                currentTc.get(i).name = name;
            } else {
                Stage stage = new Stage();
                stage.rally = rally;
                stage.position = currentTc.size() + 1;
                stage.type = StageType.TC;
                stage.name = name;
                rally.stages.add(stage);
                currentTc = rally.tcStages();
            }
        }
        while (currentTc.size() > desiredStages.size()) {
            Stage extra = currentTc.get(currentTc.size() - 1);
            removeStage(rally, extra);
            currentTc = rally.tcStages();
        }

        List<Entrant> currentEntrants = new ArrayList<>(rally.entrants);
        if (request.entrants().size() > currentEntrants.size()
                && resultRepository.countByRallyId(rally.id) > 0) {
            throw badRequest("No se pueden añadir pilotos con el rally ya empezado");
        }
        for (int i = 0; i < request.entrants().size(); i++) {
            RallyConfigRequest.EntrantInput input = request.entrants().get(i);
            if (input == null || blank(input.name())) {
                throw badRequest("Todos los pilotos necesitan nombre");
            }
            if (i < currentEntrants.size()) {
                currentEntrants.get(i).name = Names.clean(input.name());
                currentEntrants.get(i).car = Names.clean(input.car());
            } else {
                Entrant entrant = new Entrant();
                entrant.rally = rally;
                entrant.position = rally.entrants.size();
                entrant.name = Names.clean(input.name());
                entrant.car = Names.clean(input.car());
                rally.entrants.add(entrant);
                currentEntrants.add(entrant);
            }
        }
        while (rally.entrants.size() > request.entrants().size()) {
            Entrant extra = rally.entrants.get(rally.entrants.size() - 1);
            removeEntrant(rally, extra);
        }

        renumberStages(rally);
        renumberEntrants(rally);
        rally.version++;
        rally.updatedAt = Instant.now();
        rallyRepository.save(rally);
        return viewService.view(rally.id);
    }

    @Transactional
    public RallyView setResult(Long rallyId, Long stageId, Long entrantId, ResultRequest request) {
        Rally rally = getRally(rallyId);
        Stage stage = rally.stages.stream()
                .filter(s -> Objects.equals(s.id, stageId))
                .findFirst()
                .orElseThrow(() -> badRequest("Tramo no encontrado"));
        Entrant entrant = rally.entrants.stream()
                .filter(e -> Objects.equals(e.id, entrantId))
                .findFirst()
                .orElseThrow(() -> badRequest("Piloto no encontrado"));

        if (entrant.retired && (entrant.retiredAtPosition == null
                || stage.position >= entrant.retiredAtPosition)) {
            throw badRequest("El piloto está retirado");
        }

        String kind = request == null || request.kind() == null
                ? "CLEAR"
                : request.kind().toUpperCase();

        StageResult result = resultRepository.findByStageIdAndEntrantId(stageId, entrantId)
                .orElse(null);

        if ("CLEAR".equals(kind)) {
            if (result != null) {
                resultRepository.delete(result);
            }
        } else if ("VALID".equals(kind)) {
            Long millis = request == null ? null : request.millis();
            if (millis == null || millis <= 0) {
                throw badRequest("Tiempo no válido");
            }
            if (result == null) {
                result = new StageResult();
                result.rallyId = rally.id;
                result.stageId = stage.id;
                result.entrantId = entrant.id;
            }
            result.kind = ResultKind.VALID;
            result.millis = millis;
            result.updatedAt = Instant.now();
            resultRepository.save(result);
        } else if ("PENALTY".equals(kind)) {
            if (stage.type == StageType.SHAKEDOWN) {
                throw badRequest("El shakedown no tiene penalizaciones");
            }
            if (result == null) {
                result = new StageResult();
                result.rallyId = rally.id;
                result.stageId = stage.id;
                result.entrantId = entrant.id;
            }
            result.kind = ResultKind.PENALTY;
            result.millis = null;
            result.updatedAt = Instant.now();
            resultRepository.save(result);
        } else {
            throw badRequest("Tipo de resultado no válido");
        }

        rally.version++;
        rally.updatedAt = Instant.now();
        rallyRepository.save(rally);
        return viewService.view(rally.id);
    }

    @Transactional
    public RallyView reopen(Long rallyId) {
        Rally rally = getRally(rallyId);
        rallyRepository.findFirstByStatusOrderByCreatedAtDesc(RallyStatus.ACTIVE)
                .filter(active -> !Objects.equals(active.id, rally.id))
                .ifPresent(active -> {
                    active.status = RallyStatus.FINISHED;
                    active.updatedAt = Instant.now();
                    active.version++;
                    rallyRepository.save(active);
                });
        rally.status = RallyStatus.ACTIVE;
        rally.updatedAt = Instant.now();
        rally.version++;
        rallyRepository.save(rally);
        return viewService.view(rally.id);
    }

    @Transactional
    public RallyView retire(Long rallyId, Long entrantId, Long stageId) {
        Rally rally = getRally(rallyId);
        Entrant entrant = rally.entrants.stream()
                .filter(e -> Objects.equals(e.id, entrantId))
                .findFirst()
                .orElseThrow(() -> badRequest("Piloto no encontrado"));
        Stage stage = rally.stages.stream()
                .filter(s -> Objects.equals(s.id, stageId))
                .findFirst()
                .orElseThrow(() -> badRequest("Tramo no encontrado"));
        if (stage.type == StageType.SHAKEDOWN) {
            throw badRequest("El shakedown no tiene retirados");
        }
        entrant.retired = true;
        entrant.retiredAtPosition = stage.position;
        for (StageResult result : resultRepository.findByRallyIdAndEntrantId(rally.id, entrantId)) {
            Stage resultStage = rally.stages.stream()
                    .filter(s -> Objects.equals(s.id, result.stageId))
                    .findFirst()
                    .orElse(null);
            if (resultStage != null && resultStage.position >= stage.position) {
                resultRepository.delete(result);
            }
        }
        rally.version++;
        rally.updatedAt = Instant.now();
        rallyRepository.save(rally);
        return viewService.view(rally.id);
    }

    @Transactional
    public RallyView unretire(Long rallyId, Long entrantId) {
        Rally rally = getRally(rallyId);
        Entrant entrant = rally.entrants.stream()
                .filter(e -> Objects.equals(e.id, entrantId))
                .findFirst()
                .orElseThrow(() -> badRequest("Piloto no encontrado"));
        entrant.retired = false;
        entrant.retiredAtPosition = null;
        rally.version++;
        rally.updatedAt = Instant.now();
        rallyRepository.save(rally);
        return viewService.view(rally.id);
    }

    @Transactional
    public void delete(Long rallyId) {
        Rally rally = getRally(rallyId);
        resultRepository.deleteByRallyId(rallyId);
        rallyRepository.delete(rally);
    }

    public Rally getRally(Long rallyId) {
        return rallyRepository.findById(rallyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rally no encontrado"));
    }

    private void removeStage(Rally rally, Stage stage) {
        resultRepository.deleteByStageId(stage.id);
        rally.stages.remove(stage);
    }

    private void removeEntrant(Rally rally, Entrant entrant) {
        resultRepository.deleteByEntrantId(entrant.id);
        rally.entrants.remove(entrant);
    }

    private void renumberStages(Rally rally) {
        List<Stage> tc = rally.stages.stream()
                .filter(s -> s.type == StageType.TC)
                .sorted(Comparator.comparingInt(s -> s.position))
                .toList();
        for (int i = 0; i < tc.size(); i++) {
            tc.get(i).position = i + 1;
        }
    }

    private void renumberEntrants(Rally rally) {
        List<Entrant> sorted = rally.entrants.stream()
                .sorted(Comparator.comparingInt(e -> e.position))
                .toList();
        for (int i = 0; i < sorted.size(); i++) {
            sorted.get(i).position = i;
        }
    }

    private PenaltyMode penaltyMode(String value) {
        if (blank(value)) {
            return PenaltyMode.WORST_PLUS;
        }
        try {
            return PenaltyMode.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw badRequest("Modo de penalización no válido");
        }
    }

    private long penaltyValue(Long value) {
        if (value == null || value < 0) {
            return 30_000L;
        }
        return value;
    }

    private String newInviteToken() {
        String token;
        do {
            token = UUID.randomUUID().toString().replace("-", "");
        } while (rallyRepository.findByInviteToken(token).isPresent());
        return token;
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
