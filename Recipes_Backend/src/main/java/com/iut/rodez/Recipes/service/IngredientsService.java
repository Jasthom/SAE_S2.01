package com.iut.rodez.Recipes.service;

import com.iut.rodez.Recipes.model.Ingredients;
import com.iut.rodez.Recipes.model.IngredientsRequest;
import com.iut.rodez.Recipes.model.IngredientsResponse;
import com.iut.rodez.Recipes.repository.IngredientsRepository;
import com.iut.rodez.Recipes.repository.IngredientRepository;
import com.iut.rodez.Recipes.repository.UnitRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Service
public class IngredientsService {

    @Autowired
    private IngredientsRepository ingredientsRepository;

    @Autowired
    private IngredientRepository ingredientRepository;

    @Autowired
    private UnitRepository unitRepository;

    public ResponseEntity<List<IngredientsResponse>> getIngredients() {
        List<Ingredients> ingredients = new ArrayList<>();
        ingredientsRepository.findAll().forEach(ingredients::add);
        List<IngredientsResponse> ingredientsResponses = new ArrayList<>();
        ingredients.forEach(ingredients1 -> {
            IngredientsResponse ing = new IngredientsResponse();
            ing.setIngredientId(ingredients1.getIngredient().getId());
            ing.setName(ingredients1.getIngredient().getName());
            ing.setCategory(ingredients1.getIngredient().getCategory());
            ing.setImage(ingredients1.getIngredient().getImage());
            ing.setQuantity(ingredients1.getQuantity());
            ing.setUnit(ingredients1.getUnit().getName());
            ingredientsResponses.add(ing);
        });
        return new ResponseEntity<>(ingredientsResponses, HttpStatus.OK);
    }

    public ResponseEntity<IngredientsResponse> getIngredientsById(String id) {
        if (!ingredientsRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        Ingredients ingredients = ingredientsRepository.findById(id).get();
        IngredientsResponse ingredientsResponse = new IngredientsResponse();
        ingredientsResponse.setIngredientId(ingredients.getIngredient().getId());
        ingredientsResponse.setName(ingredients.getIngredient().getName());
        ingredientsResponse.setCategory(ingredients.getIngredient().getCategory());
        ingredientsResponse.setImage(ingredients.getIngredient().getImage());
        ingredientsResponse.setQuantity(ingredients.getQuantity());
        ingredientsResponse.setUnit(ingredients.getUnit().getName());
        return new ResponseEntity<>(ingredientsResponse, HttpStatus.OK);
    }

    public ResponseEntity<Ingredients> postIngredients(IngredientsRequest ingredientsRequest) {
        if (!ingredientRepository.existsById(ingredientsRequest.getIngredientId())
            || ingredientsRequest.getQuantity() <= 0.0
            || !unitRepository.existsById(ingredientsRequest.getUnitId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        Ingredients ingredients = new Ingredients();
        ingredients.setIngredient(ingredientRepository.findById(ingredientsRequest.getIngredientId()).get());
        ingredients.setQuantity(ingredientsRequest.getQuantity());
        ingredients.setUnit(unitRepository.findById(ingredientsRequest.getUnitId()).get());
        ingredientsRepository.save(ingredients);
        return new ResponseEntity<>(ingredients, HttpStatus.CREATED);
    }

    public ResponseEntity<HttpStatus> deleteIngredients(String id) {
        if (!ingredientsRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        ingredientsRepository.deleteById(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    public ResponseEntity<Ingredients> putIngredients(IngredientsRequest ingredientsRequest, String id) {
        if (!ingredientsRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        Ingredients ingredients = ingredientsRepository.findById(id).get();
        if (!ingredientRepository.existsById(ingredientsRequest.getIngredientId())
                || ingredientsRequest.getQuantity() <= 0.0
                || !unitRepository.existsById(ingredientsRequest.getUnitId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        ingredients.setIngredient(ingredientRepository.findById(ingredientsRequest.getIngredientId()).get());
        ingredients.setQuantity(ingredientsRequest.getQuantity());
        ingredients.setUnit(unitRepository.findById(ingredientsRequest.getUnitId()).get());
        ingredientsRepository.save(ingredients);
        return new ResponseEntity<>(ingredients, HttpStatus.OK);
    }
}
