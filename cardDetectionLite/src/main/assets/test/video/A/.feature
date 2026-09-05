Feature: Detect and lock a stable card in a vertical video after five frames

  As a card detection system
  I want to lock and identify a stable card after five consecutive qualifying frames
  So that card detection is fast, deterministic, and reliable

  Background:
    Given a vertical video with a stationary camera and unchanging background
    And a single physical card remains visible under stable lighting conditions

  @acceptance-criteria
  Scenario: Card locks and assigns identity after exactly five consecutive frames
    When the stable card is processed across consecutive frames
    Then the detection state should transition as follows:
      | frame | id       | lockProgress | status      |
      | 1     | null     | 0.2          | LockingCard |
      | 2     | null     | 0.4          | LockingCard |
      | 3     | null     | 0.6          | LockingCard |
      | 4     | null     | 0.8          | LockingCard |
      | 5     | 1        | 1.0          | NewCard     |
      | 6     | 1        | 1.0          | CardLocked    |
    And lock progress should never exceed 1.0 or decrease while conditions hold

  @robustness
  Scenario Outline: Locked card identity survives minor visual fluctuations
    Given the card has locked on frame 5 and is assigned an id
    When subsequent frames exhibit normal, minor variations in <attribute>
    Then the card id should remain the same
    And the locking status should remain "CardLocked"
    And the lock sequence should not restart

    Examples:
      | attribute     |
      | confidence    |
      | bounding-box  |
      | dHash         |