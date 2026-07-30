package com.grash.server.controller;

import com.grash.server.service.WordBankService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/themes")
public class ThemeController {

    private final WordBankService wordBankService;

    public ThemeController(WordBankService wordBankService) {
        this.wordBankService = wordBankService;
    }

    @GetMapping
    public List<String> listThemes() {
        return wordBankService.listThemes();
    }
}
