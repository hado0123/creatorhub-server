package com.creatorhub.repository;

import com.creatorhub.entity.FileObject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface FileObjectRepository extends JpaRepository<FileObject, Long> {
    List<FileObject> findByStorageKeyIn(Collection<String> storageKeys);
    List<FileObject> findByStorageKeyStartingWith(String baseKey);
}
