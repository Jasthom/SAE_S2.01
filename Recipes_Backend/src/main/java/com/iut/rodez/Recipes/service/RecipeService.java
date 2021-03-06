package com.iut.rodez.Recipes.service;

import com.iut.rodez.Recipes.model.*;
import com.iut.rodez.Recipes.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.logging.log4j.util.Strings.isBlank;

@Service
public class RecipeService {

    @Autowired
    private RecipeRepository recipeRepository;

    @Autowired
    private IngredientsService ingredientsService;

    @Autowired
    private TypeRepository typeRepository;

    @Autowired
    private IngredientRepository ingredientRepository;

    @Autowired
    private UnitRepository unitRepository;

    @Autowired
    private IngredientsRepository ingredientsRepository;

    @Autowired
    private StepRepository stepRepository;

    public ResponseEntity<List<RecipeShortResponse>> getRecipes(String name, List<String> ids_type_recipe) {
        List<Recipe> recipes = new ArrayList<>();
        recipeRepository.findAll().forEach(recipes::add);
        recipes = recipes
                .stream()
                .filter(recipe -> ids_type_recipe.isEmpty() || ids_type_recipe.contains(recipe.getType().getId()))
                .filter(recipe -> isBlank(name) || recipe.getName().toLowerCase().contains(name.toLowerCase()))
                .collect(Collectors.toList());
        List<RecipeShortResponse> recipeShortResponses = new ArrayList<>();
        recipes.forEach(recipe -> {
            RecipeShortResponse recipeShortResponse = new RecipeShortResponse();
            recipeShortResponse.setId(recipe.getId());
            recipeShortResponse.setName(recipe.getName());
            recipeShortResponse.setTime(recipe.getTime());
            recipeShortResponse.setImage(recipe.getImage());
            recipeShortResponses.add(recipeShortResponse);
        });
        return new ResponseEntity<>(recipeShortResponses, HttpStatus.OK);
    }

    public ResponseEntity<RecipeResponse> getRecipeById(String id) {
        if (!recipeRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        Recipe recipe = recipeRepository.findById(id).get();
        RecipeResponse recipeResponse = new RecipeResponse();
        recipeResponse.setId(recipe.getId());
        recipeResponse.setName(recipe.getName());
        recipeResponse.setTime(recipe.getTime());
        recipeResponse.setType(recipe.getType());
        List<IngredientsResponse> ingredients = new ArrayList<>();
        recipe.getIngredients().forEach(ing -> ingredients.add(ingredientsService.getIngredientsById(ing.getId()).getBody()));
        recipeResponse.setIngredients(ingredients);
        List<Step> steps = new ArrayList<>(recipe.getSteps());
        recipe.getSteps().forEach(step -> steps.set(step.getStep_order() - 1, step));
        recipeResponse.setSteps(steps);
        recipeResponse.setPeople(recipe.getNumber_person());
        recipeResponse.setImage(recipe.getImage());
        return new ResponseEntity<>(recipeResponse, HttpStatus.OK);
    }

    public ResponseEntity<RecipeResponse> postRecipe(RecipeRequest recipeRequest) {
        if (isBlank(recipeRequest.getName())
            || recipeRequest.getPeople() <= 0
            || recipeRequest.getTime() <= 0
            || !typeRepository.existsById(recipeRequest.getTypeId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        List<String> names = new ArrayList<>();
        recipeRepository.findAll().forEach(recipe -> names.add(recipe.getName()));
        if (names.contains(recipeRequest.getName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
        List<Ingredients> ingredients = new ArrayList<>();
        recipeRequest.getIngredients().forEach(ingredientsRequest -> {
            if (!ingredientRepository.existsById(ingredientsRequest.getIngredientId())
                || ingredientsRequest.getQuantity() <= 0.0
                || !unitRepository.existsById(ingredientsRequest.getUnitId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
            } else {
                Ingredients ing = new Ingredients();
                ing.setIngredient(ingredientRepository.findById(ingredientsRequest.getIngredientId()).get());
                ing.setQuantity(ingredientsRequest.getQuantity());
                ing.setUnit(unitRepository.findById(ingredientsRequest.getUnitId()).get());
                ingredientsRepository.save(ing);
                ingredients.add(ing);
            }
        });
        List<Step> steps = new ArrayList<>(recipeRequest.getSteps());
        recipeRequest.getSteps().forEach(step -> {
            if (isBlank(step.getDescr())
                || step.getStep_order() <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
            } else {
                stepRepository.save(step);
                steps.set(step.getStep_order() - 1, step);
            }
        });
        Recipe recipe = new Recipe();
        recipe.setName(recipeRequest.getName());
        recipe.setNumber_person(recipeRequest.getPeople());
        recipe.setTime(recipeRequest.getTime());
        recipe.setImage(recipeRequest.getImage());
        recipe.setType(typeRepository.findById(recipeRequest.getTypeId()).get());
        recipe.setIngredients(ingredients);
        recipe.setSteps(steps);
        recipeRepository.save(recipe);
        RecipeResponse recipeResponse = getRecipeById(recipe.getId()).getBody();
        return new ResponseEntity<>(recipeResponse, HttpStatus.CREATED);
    }

    public ResponseEntity<HttpStatus> deleteRecipe(String id) {
        if (!recipeRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        Recipe recipe = recipeRepository.findById(id).get();
        List<String> ingredientsIds = new ArrayList<>();
        recipe.getIngredients().forEach(ingredients -> ingredientsIds.add(ingredients.getId()));
        recipe.getSteps().forEach(step -> stepRepository.deleteById(step.getId()));
        recipeRepository.deleteById(id);
        ingredientsIds.forEach(ingredientsId -> ingredientsRepository.deleteById(ingredientsId));
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    public ResponseEntity<RecipeResponse> putRecipe(RecipeRequest recipeRequest, String id) {
        if (!recipeRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        Recipe recipe = recipeRepository.findById(id).get();
        if (isBlank(recipeRequest.getName())
                || recipeRequest.getPeople() <= 0
                || recipeRequest.getTime() <= 0
                || !typeRepository.existsById(recipeRequest.getTypeId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        List<String> names = new ArrayList<>();
        recipeRepository.findAll().forEach(rec -> names.add(rec.getName()));
        if (!recipeRequest.getName().equals(recipe.getName()) && names.contains(recipeRequest.getName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
        List<Ingredients> ingredients = new ArrayList<>();
        recipeRequest.getIngredients().forEach(ingredientsRequest -> {
            if (!ingredientRepository.existsById(ingredientsRequest.getIngredientId())
                    || ingredientsRequest.getQuantity() <= 0.0
                    || !unitRepository.existsById(ingredientsRequest.getUnitId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
            } else {
                Ingredients ing = new Ingredients();
                ing.setIngredient(ingredientRepository.findById(ingredientsRequest.getIngredientId()).get());
                ing.setQuantity(ingredientsRequest.getQuantity());
                ing.setUnit(unitRepository.findById(ingredientsRequest.getUnitId()).get());
                ingredientsRepository.save(ing);
                ingredients.add(ing);
            }
        });
        List<Step> steps = new ArrayList<>(recipeRequest.getSteps());
        recipeRequest.getSteps().forEach(step -> {
            if (isBlank(step.getDescr())
                    || step.getStep_order() <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
            } else {
                stepRepository.save(step);
                steps.set(step.getStep_order() - 1, step);
            }
        });
        List<String> ingredientsIds = new ArrayList<>();
        recipe.getIngredients().forEach(ing -> ingredientsIds.add(ing.getId()));
        recipe.getSteps().forEach(step -> stepRepository.deleteById(step.getId()));
        recipe.setName(recipeRequest.getName());
        recipe.setNumber_person(recipeRequest.getPeople());
        recipe.setTime(recipeRequest.getTime());
        recipe.setImage(recipeRequest.getImage());
        recipe.setType(typeRepository.findById(recipeRequest.getTypeId()).get());
        recipe.setIngredients(ingredients);
        recipe.setSteps(steps);
        recipeRepository.save(recipe);
        ingredientsIds.forEach(ingredientsId -> ingredientsRepository.deleteById(ingredientsId));
        RecipeResponse recipeResponse = getRecipeById(recipe.getId()).getBody();
        return new ResponseEntity<>(recipeResponse, HttpStatus.OK);
    }

    public ResponseEntity<List<RecipeShortResponse>> getRecipeForHomepage() {
        List<Recipe> recipes = new ArrayList<>();
        recipeRepository.findAll().forEach(recipes::add);
        List<RecipeShortResponse> recipeShortResponses = new ArrayList<>(0);
        while (recipeShortResponses.size() < 8 && recipeShortResponses.size() < recipes.size()) {
            Recipe recipe = recipes.get(recipeShortResponses.size());
            RecipeShortResponse recipeShortResponse = new RecipeShortResponse();
            recipeShortResponse.setId(recipe.getId());
            recipeShortResponse.setName(recipe.getName());
            recipeShortResponse.setTime(recipe.getTime());
            recipeShortResponse.setImage(recipe.getImage());
            recipeShortResponses.add(recipeShortResponse);
        }
        return new ResponseEntity<>(recipeShortResponses, HttpStatus.OK);
    }
}
