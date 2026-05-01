package com.example.authserver.controller.admin;

import com.example.authserver.dto.UserForm;
import com.example.authserver.entity.User;
import com.example.authserver.service.EmailService;
import com.example.authserver.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;
    private final EmailService emailService;

    @GetMapping
    public String listUsers(@RequestParam(defaultValue = "") String q,
                            @RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "20") int size,
                            Model model) {
        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        var users = q.isBlank()
                ? userService.findAll(pageable)
                : userService.search(q, pageable);

        model.addAttribute("users", users);
        model.addAttribute("q", q);
        return "admin/users/list";
    }

    @GetMapping("/new")
    public String newUserForm(Model model) {
        model.addAttribute("form", new UserForm());
        model.addAttribute("allRoles", userService.findAllRoles());
        return "admin/users/form";
    }

    @PostMapping("/new")
    public String createUser(@Valid @ModelAttribute("form") UserForm form,
                             BindingResult result,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("allRoles", userService.findAllRoles());
            return "admin/users/form";
        }
        try {
            User user = userService.createUser(form);
            emailService.sendWelcomeEmail(user.getEmail(), user.getUsername());
            redirectAttributes.addFlashAttribute("success", "User created: " + user.getUsername());
            return "redirect:/admin/users";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("allRoles", userService.findAllRoles());
            return "admin/users/form";
        }
    }

    @GetMapping("/{id}/edit")
    public String editUserForm(@PathVariable Long id, Model model) {
        User user = userService.findById(id);
        UserForm form = new UserForm();
        form.setId(user.getId());
        form.setUsername(user.getUsername());
        form.setEmail(user.getEmail());
        form.setEnabled(user.isEnabled());
        form.setRoles(new java.util.HashSet<>(
                user.getRoles().stream().map(r -> r.getName()).toList()
        ));
        model.addAttribute("form", form);
        model.addAttribute("user", user);
        model.addAttribute("allRoles", userService.findAllRoles());
        return "admin/users/form";
    }

    @PostMapping("/{id}/edit")
    public String updateUser(@PathVariable Long id,
                             @Valid @ModelAttribute("form") UserForm form,
                             BindingResult result,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        form.setId(id);
        if (result.hasErrors()) {
            model.addAttribute("allRoles", userService.findAllRoles());
            return "admin/users/form";
        }
        try {
            userService.updateUser(id, form);
            redirectAttributes.addFlashAttribute("success", "User updated.");
            return "redirect:/admin/users";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("allRoles", userService.findAllRoles());
            return "admin/users/form";
        }
    }

    @PostMapping("/{id}/delete")
    public String deleteUser(@PathVariable Long id,
                             RedirectAttributes redirectAttributes) {
        userService.deleteUser(id);
        redirectAttributes.addFlashAttribute("success", "User deleted.");
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/toggle-lock")
    public String toggleLock(@PathVariable Long id,
                             RedirectAttributes redirectAttributes) {
        User user = userService.findById(id);
        UserForm form = new UserForm();
        form.setId(user.getId());
        form.setUsername(user.getUsername());
        form.setEmail(user.getEmail());
        form.setEnabled(!user.isEnabled()); // toggle
        form.setRoles(new java.util.HashSet<>(
                user.getRoles().stream().map(r -> r.getName()).toList()
        ));
        userService.updateUser(id, form);
        redirectAttributes.addFlashAttribute("success",
                "User " + (form.isEnabled() ? "enabled" : "disabled") + ".");
        return "redirect:/admin/users";
    }
}
