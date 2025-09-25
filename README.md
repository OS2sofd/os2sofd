When many KLEs are in use on an OU and/or Person.Affiliations, then the GROUP_CONCAT sql command might reach the 1024 byte limit,
so it is recommended to increase the value

group_concat_max_len = 10240

the default is 1024
