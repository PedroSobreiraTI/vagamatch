package com.vagamatch.vagaservice.controller;

import com.vagamatch.vagaservice.dto.SkillResponse;
import com.vagamatch.vagaservice.service.SkillService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/skills")
@Tag(name = "Skills", description = "Catálogo de skills conhecidas pelo sistema")
public class SkillController {

    private final SkillService skillService;

    public SkillController(SkillService skillService) {
        this.skillService = skillService;
    }

    @GetMapping
    @Operation(summary = "Lista todas as skills cadastradas")
    public List<SkillResponse> listar() {
        return skillService.listar();
    }
}
