package com.skillbridge.repository;

// Temporarily commented out until Engineer entity is properly implemented
// This repository will be enabled once the Engineer entity is fully defined with JPA annotations

import com.skillbridge.entity.Engineer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EngineersRepository extends JpaRepository<Engineer, Integer> {

    /*
     This method returns the number of engineers by their statuses
     */
    Long countEngineersByStatus(String status);

//    @Query("select count(status) as Count_of_available_engineers\n" +
//            "from engineers e\n" +
//            "where e.status = 'AVAILABLE'")
//    List<Engineer> findFeaturedEngineers();

}

