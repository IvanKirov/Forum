package javazoo.forum.controller;

import javazoo.forum.entity.Category;
import javazoo.forum.entity.Question;
import javazoo.forum.entity.Subcategory;
import javazoo.forum.entity.Tag;
import javazoo.forum.repository.CategoryRepository;
import javazoo.forum.repository.QuestionRepository;
import javazoo.forum.repository.SubcategoryRepository;
import javazoo.forum.repository.TagRepository;
import javazoo.forum.viewModel.SubcategoryViewModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class CategoryController {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private SubcategoryRepository subcategoryRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private TagRepository tagRepository;


    @GetMapping("categories/{id}")
    public String openCategory(@PathVariable Integer id, @PageableDefault(value = 6) Pageable pageable, Model model) {

        List<Category> categories = this.categoryRepository.findAllByOrderByOrderNoAsc();
        List<Subcategory> subcategories = this.subcategoryRepository.findAllByOrderByOrderNoAsc();
        Category category = this.categoryRepository.findOne(id);
        Page<Question> questions = this.questionRepository.findByCategoryOrderByCreationDateDescLastAnswerDesc(category, pageable);
        List<Tag> allTags = this.tagRepository.findAll();
        allTags.sort((Tag t1,Tag t2)-> t2.getQuestions().size()-t1.getQuestions().size());
        List<Tag> tags = allTags.stream().limit(20).collect(Collectors.toList());

        model.addAttribute("categories", categories);
        model.addAttribute("questions", questions);
        model.addAttribute("subcategories", subcategories);
        model.addAttribute("categoryId", id);
        model.addAttribute("view", "categories/categories");
        model.addAttribute("tags", tags);
        model.addAttribute("size", 6);
        return "base-layout";
    }

    @GetMapping("categories/{categoryId}/{subcategoryId}")
    public String openSubCategory(@PathVariable Integer categoryId, @PageableDefault(value = 6) Pageable pageable,
                                  @PathVariable Integer subcategoryId, Model model) {

        List<Category> categories = this.categoryRepository.findAllByOrderByOrderNoAsc();
        List<Subcategory> subcategories = this.subcategoryRepository.findAllByOrderByOrderNoAsc();
        Subcategory subcategory = this.subcategoryRepository.findOne(subcategoryId);
        Page<Question> questions = this.questionRepository.findBySubcategoryOrderByCreationDateDescLastAnswerDesc(subcategory, pageable);
        List<Tag> allTags = this.tagRepository.findAll();
        allTags.sort((Tag t1,Tag t2)-> t2.getQuestions().size()-t1.getQuestions().size());
        List<Tag> tags = allTags.stream().limit(20).collect(Collectors.toList());

        model.addAttribute("categories", categories);
        model.addAttribute("subcategories", subcategories);
        model.addAttribute("questions", questions);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("subcategoryId", subcategoryId);
        model.addAttribute("tags", tags);
        model.addAttribute("view", "categories/categories");
        model.addAttribute("size", 6);
        return "base-layout";
    }

    @RequestMapping(value = "/subcategories")
    @ResponseBody
    public List getSubcategories(@RequestParam Integer catId) {
        List<SubcategoryViewModel> subcategories = new ArrayList<>();
        Category category = this.categoryRepository.findOne(catId);
        List<Subcategory> subs = this.subcategoryRepository.findByCategory(category);
        for (int i = 0; i < subs.size(); i++) {
            SubcategoryViewModel sub = new SubcategoryViewModel();
            sub.setId(subs.get(i).getId());
            sub.setName(subs.get(i).getName());
            subcategories.add(sub);
        }
        return subcategories;

    }

}
