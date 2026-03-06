package ch.ruppen.danceschool.school;

import org.springframework.data.jpa.repository.JpaRepository;

interface SchoolRepository extends JpaRepository<School, Long> {
}
