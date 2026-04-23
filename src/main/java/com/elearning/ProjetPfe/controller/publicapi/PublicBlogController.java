package com.elearning.ProjetPfe.controller.publicapi;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.elearning.ProjetPfe.dto.admin.BlogPostDto;
import com.elearning.ProjetPfe.service.admin.BlogService;

@RestController
@RequestMapping("/api/public/blog")
public class PublicBlogController {

    @Autowired
    private BlogService blogService;

    @GetMapping("/published")
    public ResponseEntity<List<BlogPostDto>> getPublishedPosts() {
        return ResponseEntity.ok(blogService.getPublishedPosts());
    }
}
