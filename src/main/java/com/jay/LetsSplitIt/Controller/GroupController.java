package com.jay.LetsSplitIt.Controller;

import com.jay.LetsSplitIt.Services.GroupService;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }



}
