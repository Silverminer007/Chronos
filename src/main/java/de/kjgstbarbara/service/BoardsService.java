package de.kjgstbarbara.service;

import lombok.Getter;
import org.springframework.stereotype.Service;

@Service
@Getter
public class BoardsService {
    private final BoardsRepository boardsRepository;

    public BoardsService(BoardsRepository boardsRepository) {
        this.boardsRepository = boardsRepository;
    }
}
