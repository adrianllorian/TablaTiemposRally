package com.tablatiempos.service;

import com.tablatiempos.dto.DictionariesView;
import com.tablatiempos.repo.EntrantRepository;
import com.tablatiempos.repo.StageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DictionaryService {

    private final StageRepository stageRepository;
    private final EntrantRepository entrantRepository;

    public DictionaryService(StageRepository stageRepository, EntrantRepository entrantRepository) {
        this.stageRepository = stageRepository;
        this.entrantRepository = entrantRepository;
    }

    @Transactional(readOnly = true)
    public DictionariesView all() {
        return new DictionariesView(
                stageRepository.findDistinctNames(),
                entrantRepository.findDistinctCars(),
                entrantRepository.findDistinctNames()
        );
    }
}
