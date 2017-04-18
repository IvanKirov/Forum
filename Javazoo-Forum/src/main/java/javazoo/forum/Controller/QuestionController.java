package javazoo.forum.controller;

import javazoo.forum.bindingModel.QuestionBindingModel;
import javazoo.forum.entity.*;
import javazoo.forum.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import javax.transaction.Transactional;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@Transactional
public class QuestionController {

    @Autowired
    private AnswersRepository answersRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private SubcategoryRepository subcategoryRepository;

    @Autowired
    private TagRepository tagRepository;

    @GetMapping("/question/create")
    @PreAuthorize("isAuthenticated()")
    public String create(Model model){

        List<Category> categories = this.categoryRepository.findAllByOrderByOrderNoAsc();
        List<Subcategory> subcategories = this.subcategoryRepository.findAllByOrderByOrderNoAsc();

        model.addAttribute("view", "question/create");
        model.addAttribute("categories", categories);
        model.addAttribute("subcategories", subcategories);

        return "base-layout";
    }

    @PostMapping("/question/create")
    @PreAuthorize("isAuthenticated()")
    public String createProcess(QuestionBindingModel questionBindingModel,Model model){

        if(questionBindingModel.getTitle().equals("")){

            List<Category> categories = this.categoryRepository.findAllByOrderByOrderNoAsc();
            List<Subcategory> subcategories = this.subcategoryRepository.findAllByOrderByOrderNoAsc();
            String error = "Please enter a valid title";
            model.addAttribute("error", error);
            model.addAttribute("content", questionBindingModel.getContent());
            model.addAttribute("categories", categories);
            model.addAttribute("subcategories", subcategories);
            model.addAttribute("view", "/question/create");
            return "base-layout";
        }

        if(questionBindingModel.getContent().equals("")){

            List<Category> categories = this.categoryRepository.findAllByOrderByOrderNoAsc();
            List<Subcategory> subcategories = this.subcategoryRepository.findAllByOrderByOrderNoAsc();
            String error = "Please enter a valid content";
            model.addAttribute("error", error);
            model.addAttribute("title", questionBindingModel.getTitle());
            model.addAttribute("categories", categories);
            model.addAttribute("subcategories", subcategories);
            model.addAttribute("view", "/question/create");
            return "base-layout";
        }

        UserDetails user = (UserDetails) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        User userEntity = this.userRepository.findByUsername(user.getUsername());
        Category category = this.categoryRepository.findOne(questionBindingModel.getCategoryId());
        Subcategory subcategory = this.subcategoryRepository.findOne(questionBindingModel.getSubcategoryId());
        HashSet<Tag> tags= this.findTagsFromString(questionBindingModel.getTagString());

        Question questionEntity = new Question(
                        questionBindingModel.getTitle(),
                        questionBindingModel.getContent(),
                        userEntity,
                        category,
                        subcategory,
                        tags
        );

        this.questionRepository.saveAndFlush(questionEntity);

        return "redirect:/";
    }

    @GetMapping("/question/{id}")
    public String details(Model model, @PathVariable Integer id){
        if (!this.questionRepository.exists(id)){
            return "redirect:/";
        }

        if(!(SecurityContextHolder.getContext().getAuthentication()
                instanceof AnonymousAuthenticationToken)){

            UserDetails principal = (UserDetails) SecurityContextHolder.getContext()
                    .getAuthentication().getPrincipal();

            User entityUser = this.userRepository.findByUsername(principal.getUsername());

            model.addAttribute("user", entityUser);
        }

        Question question = this.questionRepository.findOne(id);

        List<Answer> answers = this.answersRepository.findByQuestion(question);
        List<Category> categories = this.categoryRepository.findAllByOrderByOrderNoAsc();
        List<Subcategory> subcategories = this.subcategoryRepository.findAllByOrderByOrderNoAsc();

        Subcategory subcategory = question.getSubcategory();
        Category category = subcategory.getCategory();

        model.addAttribute("question", question);
        model.addAttribute("answers", answers);
        model.addAttribute("subcategoryId", subcategory.getId());
        model.addAttribute("subcategories", subcategories);
        model.addAttribute("categories", categories);
        model.addAttribute("categoryId", category.getId());
        model.addAttribute("view", "question/details");


        return "base-layout";
    }

    @GetMapping("/question/edit/{id}")
    @PreAuthorize("isAuthenticated()")
    public String edit(@PathVariable Integer id, Model model){
        if(!this.questionRepository.exists(id)){
            return "redirect:/";
        }

        Question question = this.questionRepository.findOne(id);

        if(!isUserAuthorOrAdmin(question)){
            return "redirect:/question/"+id;
        }

        List<Category> categories = this.categoryRepository.findAllByOrderByOrderNoAsc();
        List<Subcategory> subcategories = this.subcategoryRepository.findAllByOrderByOrderNoAsc();

        String tagString = question.getTags().stream()
                .map(Tag::getName)
                .collect(Collectors.joining(", "));

        model.addAttribute("view", "question/edit");
        model.addAttribute("question", question);
        model.addAttribute("tags", tagString);
        model.addAttribute("categories", categories);
        model.addAttribute("subcategories", subcategories);

        return "base-layout";
    }

    @PostMapping("question/edit/{id}")
    @PreAuthorize("isAuthenticated()")
    public String editProcess(@PathVariable Integer id, QuestionBindingModel questionBindingModel){
        if (!this.questionRepository.exists(id)){
            return "redirect:/";
        }

        Question question = this.questionRepository.findOne(id);

        if(!isUserAuthorOrAdmin(question)){
            return "redirect:/question/"+id;
        }

        Category category = this.categoryRepository.findOne(questionBindingModel.getCategoryId());
        Subcategory subcategory = this.subcategoryRepository.findOne(questionBindingModel.getSubcategoryId());

        HashSet<Tag> tags = this.findTagsFromString(questionBindingModel.getTagString());

        question.setContent(questionBindingModel.getContent());
        question.setTitle(questionBindingModel.getTitle());
        question.setCategory(category);
        question.setSubcategory(subcategory);
        question.setTags(tags);

        this.questionRepository.saveAndFlush(question);

        return "redirect:/question/" + question.getId();
    }

    @GetMapping("/question/delete/{id}")
    @PreAuthorize("isAuthenticated()")
    public String delete(Model model, @PathVariable Integer id){
        if(!this.questionRepository.exists(id)){
            return "redirect:/";
        }

        Question question = this.questionRepository.findOne(id);

        if (!isUserAuthorOrAdmin(question)){
            return "redirect:/question/" + id;
        }

        model.addAttribute("question", question);
        model.addAttribute("view", "question/delete");

        return "base-layout";
    }

    @PostMapping("/question/delete/{id}")
    @PreAuthorize("isAuthenticated()")
    public String deleteProcess(@PathVariable Integer id){
        if(!this.questionRepository.exists(id)){
            return "redirect:/";
        }
        Question question = this.questionRepository.findOne(id);

        if (!isUserAuthorOrAdmin(question)){
            return "redirect:/question/" + id;
        }

        this.questionRepository.delete(question);

        return "redirect:/";
    }


    private boolean isUserAuthorOrAdmin(Question question) {
        UserDetails user = (UserDetails) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        User userEntity = this.userRepository.findByUsername(user.getUsername());

        return userEntity.isAdmin() || userEntity.isAuthor(question);
    }

    private HashSet<Tag> findTagsFromString(String tagString){

        if ("".equals(tagString)) return null;

        HashSet<Tag> tags = new HashSet<>();

        String[] tagNames = tagString.split("[,\\s]+");

        for (String tagName:tagNames) {
            Tag currentTag = this.tagRepository.findByName(tagName);

            if (currentTag==null){
                currentTag = new Tag(tagName);
                this.tagRepository.saveAndFlush(currentTag);
            }

            tags.add(currentTag);
        }
        return tags;
    }
}
