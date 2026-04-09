package org.rap.algotutorbe.iam.application.mapper;

import org.mapstruct.Mapper;
import org.rap.algotutorbe.common.config.GlobalMapperConfig;
import org.rap.algotutorbe.iam.application.dto.UserResponse;
import org.rap.algotutorbe.iam.domain.model.User;

@Mapper(config = GlobalMapperConfig.class)
public interface UserMapper {
    UserResponse toResponse(User user);
}
